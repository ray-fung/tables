package org.opendatakit.tables.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ColumnUtil;
import org.opendatakit.tables.utils.TableUtil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Wrapper class for UserTable that presents the table in the way that the
 * configuration says the UserTable should be presented.
 *
 * @author Administrator
 *
 */
public class SpreadsheetUserTable {
  @SuppressWarnings("unused")
  private static final String TAG = "SpreadsheetUserTable";

  private final String[] header;
  private final String[] spreadsheetIndexToElementKey;
  private final int[] spreadsheetIndexToUserTableIndexRemap;
  private final Map<String, Integer> elementKeyToSpreadsheetIndex;
  private final ArrayList<ColumnDefinition> orderedDefns;
  private final UserTable table;

  public SpreadsheetUserTable(UserTable table) {
    this.table = table;
    Context context = Tables.getInstance().getApplicationContext();

    ArrayList<String> colOrder;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, table.getAppName());
      List<Column> columns = ODKDatabaseUtils.get().getUserDefinedColumns(db, table.getTableId());
      orderedDefns = ColumnDefinition.buildColumnDefinitions(columns);
      colOrder = TableUtil.get().getColumnOrder(db, table.getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }

    if (colOrder.isEmpty()) {
      for (ColumnDefinition cd : orderedDefns) {
        if ( cd.isUnitOfRetention() ) {
          colOrder.add(cd.getElementKey());
        }
      }
    }

    header = new String[colOrder.size()];
    spreadsheetIndexToUserTableIndexRemap = new int[colOrder.size()];
    spreadsheetIndexToElementKey = new String[colOrder.size()];
    elementKeyToSpreadsheetIndex = new HashMap<String, Integer>();
    db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, table.getAppName());
      for (int i = 0; i < colOrder.size(); ++i) {
        String elementKey = colOrder.get(i);
        spreadsheetIndexToUserTableIndexRemap[i] = this.table
            .getColumnIndexOfElementKey(elementKey);

        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db, table.getTableId(),
            elementKey);

        header[i] = localizedDisplayName;
        spreadsheetIndexToElementKey[i] = elementKey;
        elementKeyToSpreadsheetIndex.put(elementKey, i);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public String getTableId() {
    return table.getTableId();
  }

  public String getAppName() {
    return table.getAppName();
  }

  public ArrayList<ColumnDefinition> getColumnDefinitions() {
    return orderedDefns;
  }

  public ColorRuleGroup getColumnColorRuleGroup(String elementKey) {
    return ColorRuleGroup.getColumnColorRuleGroup(Tables.getInstance().getApplicationContext(),
        getAppName(), getTableId(), elementKey);
  }

  public ColorRuleGroup getStatusColumnRuleGroup() {
    return ColorRuleGroup.getStatusColumnRuleGroup(Tables.getInstance().getApplicationContext(),
        getAppName(), getTableId());
  }

  public ColorRuleGroup getTableColorRuleGroup() {
    return ColorRuleGroup.getTableColorRuleGroup(Tables.getInstance().getApplicationContext(),
        getAppName(), getTableId());
  }

  int getNumberOfRows() {
    return table.getNumberOfRows();
  }

  public Row getRowAtIndex(int index) {
    return table.getRowAtIndex(index);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Whether or not we have a frozen column...

  public String getIndexedColumnElementKey() {
    String indexColumn;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(
          Tables.getInstance().getApplicationContext(), 
          getAppName());
      indexColumn = TableUtil.get().getIndexColumn(db, getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    return indexColumn;
  }

  boolean isIndexed() {
    return getIndexedColumnElementKey() != null && getIndexedColumnElementKey().length() != 0;
  }

  // ///////////////////////////////////
  // These need to be re-worked...

  public boolean hasData() {
    return !(table == null || (spreadsheetIndexToUserTableIndexRemap.length == 0));
  }

  public static class SpreadsheetCell {
    public int rowNum; // of the row
    public Row row; // the row
    public String elementKey; // of the column
    public String displayText;
    public String value;
  };

  public SpreadsheetCell getSpreadsheetCell(Context context, CellInfo cellInfo) {
    SpreadsheetCell cell = new SpreadsheetCell();
    cell.rowNum = cellInfo.rowId;
    cell.row = getRowAtIndex(cellInfo.rowId);
    cell.elementKey = cellInfo.elementKey;
    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    ColumnDefinition cd = ColumnDefinition.find(orderedDefns, cellInfo.elementKey);
    cell.displayText = cell.row.getDisplayTextOfData(context, cd.getType(), cellInfo.elementKey,
        true);
    cell.value = cell.row.getRawDataOrMetadataByElementKey(cellInfo.elementKey);
    return cell;
  }

  public ColumnDefinition getColumnByIndex(int headerCellNum) {
    return getColumnByElementKey(spreadsheetIndexToElementKey[headerCellNum]);
  }

  public ColumnDefinition getColumnByElementKey(String elementKey) {
    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    return ColumnDefinition.find(orderedDefns, elementKey);
  }

  public int getWidth() {
    return spreadsheetIndexToUserTableIndexRemap.length;
  }

  Integer getColumnIndexOfElementKey(String elementKey) {
    return elementKeyToSpreadsheetIndex.get(elementKey);
  }

  public int getNumberOfDisplayColumns() {
    return spreadsheetIndexToUserTableIndexRemap.length;
  }

  String getHeader(int colNum) {
    return header[colNum];
  }
}
