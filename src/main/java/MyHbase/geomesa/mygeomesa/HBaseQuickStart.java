package MyHbase.geomesa.mygeomesa;

import org.apache.commons.cli.ParseException; 
import org.locationtech.geomesa.hbase.data.HBaseDataStoreFactory;

import MyHbase.geomesa.data.GDELTData;

public class HBaseQuickStart extends GeoMesaQuickStart {

    // uses gdelt data
    public HBaseQuickStart(String[] args) throws ParseException {
        super(args, new HBaseDataStoreFactory().getParametersInfo(), new GDELTData());
    }

    public static void main(String[] args) {
        try {
        	 String[] params=new String[4];
        	 params[0]="--hbase.zookeepers";
        	 params[1]="master";
        	 params[2]="--hbase.catalog";
        	 params[3]="geomesa_hbase_test1";        	 
            new HBaseQuickStart(params).run();
        } catch (ParseException e) {
            System.exit(1);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(2);
        }
        System.exit(0);
    }
}
