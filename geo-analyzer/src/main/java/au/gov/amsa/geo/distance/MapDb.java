package au.gov.amsa.geo.distance;

import java.io.File;
import java.io.IOException;

import org.mapdb.DB;
import org.mapdb.DBMaker;

public enum MapDb {

	INSTANCE;

	private DB db;

	private MapDb() {
		try {
			File file = File.createTempFile("geo-analyzer", ".db");
			db = DBMaker.newFileDB(file).asyncWriteEnable().cacheSize(100000)
					.closeOnJvmShutdown().transactionDisable().make();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public DB getDb() {
		return db;
	}

}
