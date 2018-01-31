import static com.google.common.base.Charsets.UTF_8;

import java.nio.ByteBuffer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.service.StorageService;

public class StorageProxyReadTest {

  private static final String KS = "system_schema";
  private static final String CFS = "keyspaces";


  public static void main(String[] args) throws Exception {
    setup();
    run();
  }

  private static void setup() {
    System.setProperty("cassandra.config", "cassandra.yaml");
    System.setProperty("cassandra.storagedir", "lib1");

    DatabaseDescriptor.daemonInitialization();
    StorageService.instance.initServer();
  }

  private static void run() throws Exception {
    /**
     * Check is all live nodes have agreed upon schema version
     */
    StorageProxy.describeSchemaVersions();

    final ColumnFamilyStore cfs = Keyspace.open(KS).getColumnFamilyStore(CFS);
    //verify that config is loaded
    System.out.println("Cluster name: " + DatabaseDescriptor.getClusterName());
    //the slice of rows to query, we want to look at all rows
    Slice slice = Slice.make(ClusteringBound.BOTTOM, ClusteringBound.TOP);
    //key for system_auth keyspace_name
    final DecoratedKey dk = dk(cfs.metadata, "system_auth");
    //prepare simple read command
    SinglePartitionReadCommand cmd = SinglePartitionReadCommand.create(cfs.metadata,
        0, dk, slice);

    doRead(cmd);

  }


  private static Row doRead(final SinglePartitionReadCommand cmd) throws Exception {
    //read from closest replica
    RowIterator itor = StorageProxy.readOne(cmd, ConsistencyLevel.ANY, System.nanoTime());
    Row row = itor.next();
    Iterable<Cell> cells = row.cells();
    for (Cell cell : cells) {
      //cell value is represented by ByteBuffer
      System.out.println(cell.column() +": " + new String(cell.value().array(), "UTF-8"));
    }
    return row;
  }

  private static DecoratedKey dk(CFMetaData cfs, String key) {
    return cfs.partitioner.decorateKey(ByteBuffer.wrap(key.getBytes(UTF_8)));
  }
}
