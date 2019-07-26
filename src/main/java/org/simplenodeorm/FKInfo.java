/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simplenodeorm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author rob
 */
public class FKInfo {

    private String targetTable;
    private String name;
    private boolean cascadeUpdate;
    private boolean cascadeDelete;
    private List<FKColumnInfo> columns = new ArrayList();

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FKColumnInfo> getColumns() {
        Collections.sort(columns);
        return columns;
    }

    public void setColumns(List<FKColumnInfo> columns) {
        this.columns = columns;
    }

    public void addColumn(String sourceColumn, String targetColumn, Integer seq) {
        FKColumnInfo fkc = new FKColumnInfo();
        fkc.setSourceColumn(sourceColumn);
        fkc.setTargetColumn(targetColumn);
        fkc.setSeq(seq);
        columns.add(fkc);
    }

    public boolean isCascadeUpdate() {
        return cascadeUpdate;
    }

    public void setCascadeUpdate(boolean cascadeUpdate) {
        this.cascadeUpdate = cascadeUpdate;
    }

    public boolean isCascadeDelete() {
        return cascadeDelete;
    }

    public void setCascadeDelete(boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }
    
    
}
