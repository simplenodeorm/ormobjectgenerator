/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simplenodeorm;

/**
 *
 * @author rob
 */
public class ColumnInfo implements Comparable<ColumnInfo>  {
   private String fieldName;
    private String columnName;
    private Integer pkindex = Integer.MAX_VALUE;
    private boolean nullable;
    private boolean autoincrement;
    private int length;
    private String type;
    private String defaultValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Integer getPkindex() {
        return pkindex;
    }

    public void setPkindex(Integer pkindex) {
        this.pkindex = pkindex;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public boolean isPrimaryKey() {
        return (pkindex < Integer.MAX_VALUE);
    }
    
    public boolean isRequired() {
        return !nullable;
    }

    public boolean isAutoincrement() {
        return autoincrement;
    }

    public void setAutoincrement(boolean autoincrement) {
        this.autoincrement = autoincrement;
    }
    
    
   @Override
    public int compareTo(ColumnInfo o) {
        int retval = pkindex.compareTo(o.getPkindex());
        
        if (retval == 0) {
            retval = this.columnName.compareTo(o.getColumnName());
        }
        
        return retval;
    }
 }
