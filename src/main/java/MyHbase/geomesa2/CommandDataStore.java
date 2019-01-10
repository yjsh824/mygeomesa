package MyHbase.geomesa2;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.locationtech.geomesa.hbase.data.HBaseDataStoreFactory;
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class CommandDataStore {
	public Options createOptions(Param[] parameters) {
		Options options = new Options();
		for (Param p : parameters) {
			if (!p.isDeprecated()) {
				Option opt = Option.builder(null).longOpt(p.getName()).argName(p.getName()).hasArg()
						.desc(p.getDescription().toString()).required(p.isRequired()).build();
				options.addOption(opt);
			}
		}
		options.addOption(Option.builder().longOpt("cleanup").desc("Delete tables after running").build());
		return options;
	}

	public org.apache.commons.cli.CommandLine parseArgs(Class<?> caller, Options options, String[] args)
			throws ParseException {
		try {
			return new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(caller.getName(), options);
			throw e;
		}
	}

	public Map<String, String> getDataStoreParams(String catalog) throws ParseException {
		String[] args = { "--hbase.catalog", catalog, "--hbase.zookeepers", "master:2181" };
		Options options = this.createOptions(new HBaseDataStoreFactory().getParametersInfo());
		CommandLine command = this.parseArgs(getClass(), options, args);
		Map<String, String> params = new HashMap<>();
		// noinspection unchecked
		for (Option opt : options.getOptions()) {
			String value = command.getOptionValue(opt.getLongOpt());
			if (value != null) {
				params.put(opt.getArgName(), value);
			}
		}
		return params;
	}

	public DataStore createHbaseDataStore(String catalog) throws IOException, ParseException {
		Map<String, String> params = this.getDataStoreParams(catalog);
		return DataStoreFinder.getDataStore(params);
	}

	public static void main(String[] args) throws IOException, ParseException {
		CommandDataStore comm = new CommandDataStore();

		DataStore dataStore = comm.createHbaseDataStore("geomesa_hbase3");
		String spec = "fid:String:index=true,name:String:index=true,type:Integer:index=true,"
				+ "telephone:String,adminCode:Integer:index=true,address:String,*geom:Point:srid=4326";
		SimpleFeatureType sft = SimpleFeatureTypes.createType("geomesa_hbase3", spec);
		dataStore.createSchema(sft);
		comm.writeFeatures(dataStore, sft, comm.getTestData());

	}
	
	
	
	/**
	解析文件
	**/
	public List<SimpleFeature> getTestData() {
	            List<SimpleFeature> features = new ArrayList<>();
	            InputStreamReader isr = null;
	            String spec = "fid:String:index=true,name:String:index=true,type:Integer:index=true,telephone:String,adminCode:Integer:index=true,address:String,*geom:Point:srid=4326";
	            SimpleFeatureType sft = SimpleFeatureTypes.createType("geomesa_hbase3", spec);
	            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sft );

	            // use apache commons-csv to parse the GDELT file
	            try {

                    System.out.println("开始生成数据");
	                for (int i=0;i<1000;i++) {
	                    try {
	                        builder.set("fid", i);
	                        builder.set("name","name"+i);
	                        builder.set("type", "type"+i);
	                        builder.set("telephone", "telephone"+i);
	                        builder.set("adminCode", "adminCode"+i);
	                        builder.set("address","address"+i);

	                        double latitude =38.123948574+3*Math.cos(6.28*i/1000) ;
	                        double longitude =128.123948574+3*Math.sin(6.28*i/1000);
	                        builder.set("geom", "POINT (" + longitude + " " + latitude + ")");
	                        builder.featureUserData(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE);
	                        SimpleFeature feature = builder.buildFeature(String.valueOf(i));
	                        features.add(feature);
	                    } catch (Exception e) {
	                    	System.out.println("Invalid GDELT record: " + e.toString() + " " +i);
	                      
	                        e.printStackTrace();
	                    }
	                }
	            }finally {
	                if (isr != null) {
	                    try {
	                        isr.close();
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        return features;
	    }

	/**
	插入数据
	**/
	public void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
	        if (features.size() > 0) {
	            System.out.println("Writing test data");
	            try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
	                         datastore.getFeatureWriterAppend(sft.getTypeName(), Transaction.AUTO_COMMIT)) {
	                for (SimpleFeature feature : features) {
	                    try {
	                        SimpleFeature toWrite = writer.next();
	                        toWrite.setAttributes(feature.getAttributes());
	                        ((FeatureIdImpl) toWrite.getIdentifier()).setID(feature.getID());
	                        toWrite.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
	                        toWrite.getUserData().putAll(feature.getUserData());
	                        writer.write();
	                    }catch (Exception e){
	                    	System.out.println("Invalid GDELT record: " + e.toString() + " " + feature.getAttributes());
	                    }
	                }
	            }

	            System.out.println("Writing test data end.");
	        }
	    }

}
