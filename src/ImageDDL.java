import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ImageDDL is a simple website crawler that utilize Java7 (nio2)/Java8
 * (Functional Programming) new Features. Also It utilize Regular Expressions
 * (regex)
 * 
 * @author mhewedy
 *
 */
public class ImageDDL {

	private static final String PIC_HOST_DOMAIN = "http://pichost.me/";
	private static final Pattern IMAGE_PAGE_URL_PATTERN = Pattern.compile("<a\\shref=\"/\\d+");
	private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("<a\\shref=\"http://.*onclick");
	
	static int totalCount = 0;
	static int incrCount = 0;

	public static void main(String[] args) {

		validateInputOrExit(args);

		int pageNumber = Integer.parseInt(args[0]);
		try {
			Files.createDirectory(Paths.get(pageNumber + ""));

			List<String> urlOfImagePagesList = getUrlOfImagesPages(pageNumber);
			totalCount = urlOfImagePagesList.size();
			
			urlOfImagePagesList
				.stream()
				.parallel()		// Notably improve performance
				.map(p -> mapPageUrlToImageUrl(p))
				.forEach(iu -> saveImage(iu, pageNumber));

		} catch (FileAlreadyExistsException ex) {
			System.err.println("Folder \"" + pageNumber + "\" already exists!");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void validateInputOrExit(String[] args) {
		if (args.length == 0 || args[0].matches("[^0-9]+")) {
			System.out.println("Usage: java ImageDDL <pageNumber> ");
			System.out.println("Download images from " + PIC_HOST_DOMAIN + "top/<pageNumber>.html");
			System.exit(1);
		}
	}

	private static List<String> getUrlOfImagesPages(int pageNo) throws IOException {

		InputStream is = getUrlInputStream(PIC_HOST_DOMAIN + "/top/" + pageNo + ".html");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		List<String> imagePaths = br.lines()
				.map(l -> IMAGE_PAGE_URL_PATTERN.matcher(l))
				.filter(m -> m.find())
				.map(m -> {
					String g = m.group();
					return g.substring(g.indexOf('"') + 2);
				})
				.collect(Collectors.toList());

		System.out.println("Proccessing ...");
		
		is.close();
		return imagePaths;
	}


	private static String mapPageUrlToImageUrl(String imagePageUrl) {
		try {
			InputStream is = getUrlInputStream(PIC_HOST_DOMAIN + imagePageUrl);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String imageUrl = br.lines()
					.map(l -> IMAGE_URL_PATTERN.matcher(l))
					.filter(m -> m.find())
					.map(m -> {
						String g = m.group();
						return g.substring(g.indexOf('"') + 1, g.lastIndexOf('"'));
					})
					.findFirst().orElse(null);
			
			is.close();
			return imageUrl;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private static void saveImage(String imageUrl, int pageNumber) {
		try {
			InputStream imageStream = getUrlInputStream(imageUrl);
			Files.copy(imageStream, 
					Paths.get(pageNumber + "/" + imageUrl.substring(imageUrl.lastIndexOf("/") + 1)));
			imageStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			incrCount++;
			System.out.print("\r" + getProcessingPrecent() + "%");
		}
	}
	
	private static InputStream getUrlInputStream(String url) throws IOException {
		URLConnection urlConnection = new URL(url).openConnection();
		return urlConnection.getInputStream();
	}

	private static int getProcessingPrecent() {
		return (int) ((incrCount / (double) totalCount) * 100);
	}
}
