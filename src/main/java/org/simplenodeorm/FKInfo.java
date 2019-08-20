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
    private String targetModel;
    private String fieldName;
    private boolean cascadeUpdate;
    private boolean cascadeDelete;
    private String sourceColumns;
    private String targetColumns;
    private String type;

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public void setTargetModel(String targetModel) {
        this.targetModel = targetModel;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
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

    public String getSourceColumns() {
        return sourceColumns;
    }

    public void setSourceColumns(String sourceColumns) {
        this.sourceColumns = sourceColumns;
    }

    public String getTargetColumns() {
        return targetColumns;
    }

    public void setTargetColumns(String targetColumns) {
        this.targetColumns = targetColumns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

 }
