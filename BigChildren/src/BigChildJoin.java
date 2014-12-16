import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


public class BigChildJoin {


	public static void main(String[] args) {
	    Path weatherPath = Paths.get("C:/Users/Endre/workspace/Join_location_BigChild/src", "ish-history_clean.txt");
	    Path densityPath = Paths.get("C:/Users/Endre/workspace/Join_location_BigChild/src", "world_population_density_constricted_centroids_tabulated.txt");


	    Charset charset = Charset.forName("ISO-8859-1");
	    try {
	      List<String> weatherLines = Files.readAllLines(weatherPath, charset);
	      List<String> densityLines = Files.readAllLines(densityPath, charset);
          
	      File file = new File("C:/Users/Endre/workspace/Join_location_BigChild/src/weatherDensityCombined.txt");
	      FileWriter fw = new FileWriter(file.getAbsoluteFile());
	      BufferedWriter bw = new BufferedWriter(fw);
			
	      for (String weatherline : weatherLines) {
	    	//System.out.println(weatherline);
	    	  String[] currentWeatherLine = weatherline.split("\t");
	    	 //System.out.println(Arrays.toString(currentWeatherLine));
	    	  
	    	  double currentSmallestDistance = 999999999;
	    	  String currentMatchingString = ""; 
	    			  
	    	  for (String densityLine : densityLines) {
	    		// System.out.println(densityLine);
	    		  String[] currentDensityLine = densityLine.split("\t");
		    	//System.out.println(Arrays.toString(currentDensityLine));
	    		  
	    		  double distance = HaversineInKM(Double.parseDouble(currentWeatherLine[2])/1000, Double.parseDouble(currentWeatherLine[3])/1000, Double.parseDouble(currentDensityLine[3]), Double.parseDouble(currentDensityLine[2]));
	    		  if(currentSmallestDistance > distance){
	    		  	
		    	  	currentMatchingString = distance + "\t" + weatherline + "\t" + densityLine;
		    	  	currentSmallestDistance = distance; 
	    		  }
	    		  
		      }
	    	  bw.write(currentMatchingString);
	    	  bw.newLine();

	    	
	      }
		bw.close();
		System.out.println("Done");
	    } catch (IOException e) {
	      System.out.println(e);
	    }
	    


	}
	
	
    static final double _eQuatorialEarthRadius = 6378.1370D;
    static final double _d2r = (Math.PI / 180D);

    public static int HaversineInM(double lat1, double long1, double lat2, double long2) {
        return (int) (1000D * HaversineInKM(lat1, long1, lat2, long2));
    }

    public static double HaversineInKM(double lat1, double long1, double lat2, double long2) {
        double dlong = (long2 - long1) * _d2r;
        double dlat = (lat2 - lat1) * _d2r;
        double a = Math.pow(Math.sin(dlat / 2D), 2D) + Math.cos(lat1 * _d2r) * Math.cos(lat2 * _d2r)
                * Math.pow(Math.sin(dlong / 2D), 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        double d = _eQuatorialEarthRadius * c;

        return d;
    }


}
