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
public class FKColumnInfo implements Comparable<FKColumnInfo>{
   private String sourceColumn;
   private String targetColumn;
   private Integer seq;

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

 
    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }
    
    @Override
    public int compareTo(FKColumnInfo o) {
        return seq.compareTo(o.getSeq());
     }
    
}
