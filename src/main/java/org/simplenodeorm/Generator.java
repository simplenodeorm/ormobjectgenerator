/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simplenodeorm;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author tucker87
 */
public class Generator {

    private Properties config;
    private Set<String> globalImportBlacklist = new HashSet();
    private Map<String, Set<String>> modelImportBlacklist = new HashMap();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: java -jar generator.jar <path-to-configuration-properties>");
        } else {
            try {
                new Generator(args[0]);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.exit(0);
    }

    public Generator(String configPath) throws Exception {
        Connection conn = null;
        ResultSet res = null;
        FileReader reader = null;
        try {
            File f = new File(configPath);
            if (f.exists() && f.isFile()) {
                config = new Properties();
                reader = new FileReader(f);
                config.load(reader);
                reader.close();
                reader = null;
            } else {
                throw new Exception("invalid configuration file - " + f.getPath());
            }

            loadImportBlackLists();
            conn = getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            String[] tableNames = config.getProperty("table.names").split(",");

            for (String tname : tableNames) {
                if (StringUtils.isNotEmpty(tname)) {
                    generateModel(tname, dmd);
                    generateMetadata(tname, dmd);
                    generateRepository(tname, dmd);
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                }
            }

            if (res != null) {
                try {
                    res.close();
                } catch (Exception ex) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private void loadImportBlackLists() {
        String s = config.getProperty("global.imported.model.ignore");

        if (StringUtils.isNotEmpty(s)) {
            for (String m : s.split(",")) {
                this.globalImportBlacklist.add(m.trim());
            }
        }

        Enumeration e = config.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();

            if (key.endsWith(".imported.model.ignore")) {
                s = config.getProperty(key);
                if (StringUtils.isNotEmpty(s)) {
                    String model = key.substring(0, key.indexOf("."));
                    for (String m : s.split(",")) {
                        Set<String> hs = modelImportBlacklist.get(model);

                        if (hs == null) {
                            modelImportBlacklist.put(model, hs = new HashSet<String>());
                        }
                        hs.add(m.trim());
                    }
                }
            }
        }
    }

    private void generateModel(String table, DatabaseMetaData dmd) throws Exception {
        PrintWriter pw = null;
        ResultSet res = null;
        try {
            File f = getModelFile(table);
            pw = new PrintWriter(f);
            pw.println("\"use strict\"");
            pw.println();
            pw.println("const Model = require('../main/Model.js');");
            pw.println();
            pw.println("class " + f.getName().replace(".js", "") + " extends Model {");
            pw.println("    constructor(metaData) {");
            pw.println("        super(metaData);");
            pw.println("    }");
            pw.println();

            res = dmd.getPrimaryKeys("", config.getProperty("db.schema"), table);
            List<ColumnInfo> pklist = new ArrayList();
            Set<String> hs = new HashSet();
            while (res.next()) {
                ColumnInfo pki = new ColumnInfo();
                pki.setColumnName(res.getString(4));
                pki.setPkindex(res.getInt(5));
                pklist.add(pki);
                hs.add(pki.getColumnName());
            }
            res.close();
            Collections.sort(pklist);

            for (ColumnInfo pki : pklist) {
                pw.println("    // primary key " + pki.getPkindex());
                pw.println("    get" + toCamelCase(pki.getColumnName(), true) + "() { return this.getFieldValue(\"" + toCamelCase(pki.getColumnName(), false) + "\"); }");
                pw.println("    set" + toCamelCase(pki.getColumnName(), true) + "(value) { this.setFieldValue(\"" + toCamelCase(pki.getColumnName(), false) + "\", value); }");
                pw.println();
            }

            res = dmd.getColumns(null, config.getProperty("db.schema"), table, "");

            while (res.next()) {
                String cname = res.getString(4);
                if (!hs.contains(cname)) {
                    pw.println("    get" + toCamelCase(cname, true) + "() { return this.getFieldValue(\"" + toCamelCase(cname, false) + "\"); }");
                    pw.println("    set" + toCamelCase(cname, true) + "(value) { this.setFieldValue(\"" + toCamelCase(cname, false) + "\", value); }");
                    pw.println();
                }
            }

            pw.println();
            res.close();
            boolean firstone = true;
            List <FKInfo> relationships = this.getRelationships(dmd, table);
            for (FKInfo fk : relationships) {
                if (firstone) {
                   pw.println("    // foreign key relationships");
                   firstone = false;
               }
               String model = toCamelCase(fk.getTargetTable(), true);
               pw.println("    get" + model + "() { return this.getFieldValue(\"" + toCamelCase(model, false) + "\"); }");
               pw.println("    set" + model + "(value) { this.setFieldValue(\"" + toCamelCase(model, false) + "\", value); }");
               pw.println();
            }

            pw.println("}");
            pw.println();

            pw.println("module.exports = function(metaData) {");
            pw.println("    return new " + f.getName().replace(".js", "") + "(metaData);");
            pw.println("};");
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex) {
            };
            try {
                if (res != null) {
                    res.close();
                }
            } catch (Exception ex) {
            };
        }
    }

    private void generateMetadata(String table, DatabaseMetaData dmd) throws Exception {
        PrintWriter pw = null;
        ResultSet res = null;
        try {
            File f = getMetaDataFile(table);
            pw = new PrintWriter(f);
            pw.println("\"use strict\"");
            pw.println();
            pw.println("const MetaData = require('../main/MetaData.js');");
            pw.println();
            pw.println("class " + f.getName().replace(".js", "") + " extends MetaData {");
            pw.println("    constructor() {");
            pw.println("        super(");
            pw.println("            '" + toCamelCase(table, true) + "', // model name");
            pw.println("            'model/" + toCamelCase(table, true) + ".js', // relative model path");
            pw.println("            '" + table + "', // table");
            pw.println("            [");

            Map<String, Integer> pkmap = new HashMap();
            res = dmd.getPrimaryKeys("", config.getProperty("db.schema"), table);
            while (res.next()) {
                pkmap.put(res.getString(4), res.getInt(5));
            }
            res.close();

            List<ColumnInfo> columns = new ArrayList();
            res = dmd.getColumns(null, config.getProperty("db.schema"), table, "");

            while (res.next()) {
                ColumnInfo ci = new ColumnInfo();

                ci.setColumnName(res.getString(4));
                ci.setFieldName(toCamelCase(ci.getColumnName(), false));
                if (pkmap.containsKey(ci.getColumnName())) {
                    ci.setPkindex(pkmap.get(ci.getColumnName()));
                }
                int sqlType = res.getInt(5);
                String type = res.getString(6);
                int len = res.getInt(7);
                int decDigits = res.getInt(9);

                if (isNumeric(sqlType)) {
                    if ((len > 0) && (decDigits > 0)) {
                        ci.setType(type + "(" + len + "," + decDigits + ")");
                    } else if ((len > 0) && isNumericLengthValid(sqlType)) {
                        ci.setType(type + "(" + len + ")");
                    } else {
                        ci.setType(type);
                    }
                } else if ((len > 0) && isLengthValid(sqlType)) {
                    ci.setLength(len);
                    ci.setType(type);
                } else {
                    ci.setType(type);
                }

                ci.setDefaultValue(res.getString(13));
                ci.setNullable(res.getBoolean(18));
                ci.setAutoincrement(res.getBoolean(23));

                columns.add(ci);
            }

            Collections.sort(columns);

            int indx = 0;
            String comma = "";
            for (ColumnInfo ci : columns) {
                pw.println("                {  //" + indx);
                pw.println("                     fieldName:" + "\"" + ci.getFieldName() + "\",");
                pw.println("                     type:" + "\"" + ci.getType() + "\",");
                pw.print("                     columnName:" + "\"" + ci.getColumnName() + "\"");

                if (ci.getLength() > 0) {
                    pw.println(",");
                    pw.print("                     length:" + "\"" + ci.getLength() + "\"");
                }

                if (ci.isRequired()) {
                    pw.println(",");
                    pw.print("                     required: true");
                }

                if (ci.isPrimaryKey()) {
                    pw.println(",");
                    pw.print("                     primaryKey: true");
                }

                if (StringUtils.isNotBlank(ci.getDefaultValue())) {
                    pw.println(",");
                    pw.print("                     defaultValue: \"" + ci.getDefaultValue() + "\"");
                }

                if (ci.isAutoincrement()) {
                    pw.println(",");
                    pw.print("                     autoIncrementGenerator: \"" + config.getProperty("global.autoincrement.generator") + "\"");
                } else {
                    String autoinc = config.getProperty(table + ".autoincrement.generator");
                    if (StringUtils.isNotEmpty(autoinc)) {
                        pw.println(",");
                        pw.print("                     autoIncrementGenerator: \"" + autoinc + "\"");
                    }
                }

                pw.println();
                if (indx < (columns.size() - 1)) {
                    pw.println("                },");
                } else {
                    pw.println("                },");
                }

                indx++;
            }

            pw.println("            ],");
            res.close();

            List<FKInfo> relationships = getRelationships(dmd, table);
            loadOneToOneRelationships(dmd, pw, relationships);
            loadOneToManyRelationships(dmd, pw, relationships);
            loadManyToManyRelationships(dmd, pw, relationships);

            pw.println("       );");
            pw.println("    }");

            pw.println("}");
            pw.println();

            pw.println("module.exports = function(metaData) {");
            pw.println("    return new " + f.getName().replace(".js", "") + "(metaData);");
            pw.println("};");
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex) {
            };
            try {
                if (res != null) {
                    res.close();
                }
            } catch (Exception ex) {
            };
        }
    }

    private List<FKInfo> getRelationships(DatabaseMetaData dmd, String table) throws Exception {
        List<FKInfo> retval = new ArrayList();

        ResultSet res = null;

        try {
            res = dmd.getImportedKeys(null, config.getProperty("db.schema"), table);
            List<FKInfo> fklist = new ArrayList();
            FKInfo fkinfo = null;
            while (res.next()) {
                String targetTable = res.getString(3);
                String model = toCamelCase(targetTable, true);
                if (!globalImportBlacklist.contains(model)) {
                    Set<String> hs2 = modelImportBlacklist.get(toCamelCase(table, true));

                    if ((hs2 == null) || !hs2.contains(model)) {
                        if ((fkinfo == null) || !targetTable.equals(fkinfo.getTargetTable())) {
                            fkinfo = new FKInfo();
                            fkinfo.setTargetTable(targetTable);
                            fkinfo.setName(res.getString("FK_NAME"));
                            fkinfo.setCascadeDelete(res.getInt("DELETE_RULE") == DatabaseMetaData.importedKeyCascade);
                            fkinfo.setCascadeDelete(res.getInt("UPDATE_RULE") == DatabaseMetaData.importedKeyCascade);
                            retval.add(fkinfo);
                        }

                        fkinfo.addColumn(res.getString("FKCOLUMN_NAME"), res.getString("PKCOLUMN_NAME"), res.getInt("KEY_SEQ"));
                    }
                }
            }

            res.close();

            res = dmd.getExportedKeys(null, config.getProperty("db.schema"), table);
            fkinfo = null;
            while (res.next()) {
                String targetTable = res.getString(3);
                String model = toCamelCase(targetTable, true);
                if (!globalImportBlacklist.contains(model)) {
                    Set<String> hs2 = modelImportBlacklist.get(toCamelCase(table, true));

                    if ((hs2 == null) || !hs2.contains(model)) {
                        if ((fkinfo == null) || !targetTable.equals(fkinfo.getTargetTable())) {
                            fkinfo = new FKInfo();
                            fkinfo.setTargetTable(targetTable);
                            fkinfo.setName(res.getString("PK_NAME"));
                            fkinfo.setCascadeDelete(res.getInt("DELETE_RULE") == DatabaseMetaData.importedKeyCascade);
                            fkinfo.setCascadeDelete(res.getInt("UPDATE_RULE") == DatabaseMetaData.importedKeyCascade);
                            retval.add(fkinfo);
                        }

                        fkinfo.addColumn(res.getString("PKCOLUMN_NAME"), res.getString("FKCOLUMN_NAME"), res.getInt("KEY_SEQ"));
                    }
                }
            }

        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception ex) {
                }
            }
        }

        return retval;
    }

    private void loadOneToOneRelationships(DatabaseMetaData dmd, PrintWriter pw, List<FKInfo> rlist) throws Exception {
        List<FKInfo> fklist = new ArrayList();

        for (FKInfo fk : rlist) {
            Set<String> pklist = this.getPrimaryKeyColumnNames(dmd, fk.getTargetTable());

            if (pklist.size() == fk.getColumns().size()) {
                fklist.add(fk);
            }

            if (fklist.isEmpty()) {
                pw.println("            [], // one-to-one relationships");
            } else {
                pw.println("            [  // one-to-one relationships");
                int indx = 0;
                for (FKInfo fki : fklist) {
                    pw.println("                { // " + indx++);
                    pw.println("                   fieldName: \"" + toCamelCase(fki.getTargetTable(), false) + "\",");
                    pw.println("                   type: 1,");
                    pw.println("                   targetModelName: \"" + toCamelCase(fki.getTargetTable(), true) + "\",");
                    pw.println("                   targetModule: \"../model/" + toCamelCase(fki.getTargetTable(), false) + ".js\",");
                    pw.println("                   targetTableName: \"" + fki.getTargetTable() + "\",");
                    pw.println("                   status: \"enabled\",");
                    pw.println("                   joinColumns: {");
                    pw.println("                       sourceColumns: \"" + getSourceColumnList(fki.getColumns()) + "\",");
                    pw.println("                       targetColumns: \"" + getTargetColumnList(fki.getColumns()) + "\",");
                    pw.println("                   }");

                    if (indx < fklist.size() - 1) {
                        pw.println("                },");
                    } else {
                        pw.println("                }");
                    }
                }
                pw.println("            ],");
            }
        }
    }

    private void loadOneToManyRelationships(DatabaseMetaData dmd, PrintWriter pw, List<FKInfo> rlist) throws Exception {
        List<FKInfo> fklist = new ArrayList();

        for (FKInfo fk : rlist) {
            Set<String> pklist = this.getPrimaryKeyColumnNames(dmd, fk.getTargetTable());

            if (pklist.size() > fk.getColumns().size()) {
                fklist.add(fk);
            }

            if (fklist.isEmpty()) {
                pw.println("            [], // one-to-many relationships");
            } else {
                pw.println("            [  // one-to-many relationships");
                int indx = 0;
                for (FKInfo fki : fklist) {
                    pw.println("                { // " + indx++);
                    pw.println("                   fieldName: \"" + toCamelCase(fki.getTargetTable(), false) + "\",");
                    pw.println("                   type: 2,");
                    pw.println("                   targetModelName: \"" + toCamelCase(fki.getTargetTable(), true) + "\",");
                    pw.println("                   targetModule: \"../model/" + toCamelCase(fki.getTargetTable(), false) + ".js\",");
                    pw.println("                   targetTableName: \"" + fki.getTargetTable() + "\",");
                    pw.println("                   status: \"enabled\",");
                    pw.println("                   joinColumns: {");
                    pw.println("                       sourceColumns: \"" + getSourceColumnList(fki.getColumns()) + "\",");
                    pw.println("                       targetColumns: \"" + getTargetColumnList(fki.getColumns()) + "\",");
                    pw.println("                   }");

                    if (indx < fklist.size() - 1) {
                        pw.println("                },");
                    } else {
                        pw.println("                }");
                    }
                }
                pw.println("            ],");
            }
        }
    }

    private void loadManyToManyRelationships(DatabaseMetaData dmd, PrintWriter pw, List<FKInfo> rlist) throws Exception {
        pw.println("            [] // many-to-many relationships");
    }

    private String getSourceColumnList(List<FKColumnInfo> columns) {
        StringBuilder retval = new StringBuilder(64);

        String comma = "";
        for (FKColumnInfo c : columns) {
            retval.append(comma);
            retval.append(c.getSourceColumn());
            comma = ",";
        }

        return retval.toString();
    }

    private String getTargetColumnList(List<FKColumnInfo> columns) {
        StringBuilder retval = new StringBuilder(64);

        String comma = "";
        for (FKColumnInfo c : columns) {
            retval.append(comma);
            retval.append(c.getTargetColumn());
            comma = ",";
        }

        return retval.toString();
    }

    private Set<String> getPrimaryKeyColumnNames(DatabaseMetaData dmd, String table) throws Exception {
        Set<String> retval = new HashSet();
        ResultSet res = null;

        try {
            res = dmd.getPrimaryKeys("", config.getProperty("db.schema"), table);
            while (res.next()) {
                retval.add(res.getString(4));
            }

        } finally {
            try {
                res.close();
            } catch (Exception ex) {
            }
        }
        return retval;
    }

    private void generateRepository(String table, DatabaseMetaData dmd) throws Exception {
        PrintWriter pw = null;
        try {
            File f = getRepositoryFile(table);
            pw = new PrintWriter(f);
            pw.println("\"use strict\"");
            pw.println();
            pw.println("const poolAlias = '" + config.getProperty("repository.pool.alias") + "';");
            pw.println("const Repository = require('../main/Repository.js');");
            pw.println();
            pw.println("class " + f.getName().replace(".js", "") + " extends Repository {");
            pw.println("    constructor(metaData) {");
            pw.println("        super(poolAlias, metaData);");
            pw.println("    }");
            pw.println();
            pw.println("    loadNamedDbOperations() {");
            pw.println("        // define named database operations here - the convention is as follows");
            pw.println("        // namedDbOperations.set('functionName', 'objectQuery')");
            pw.println("        // example: namedDbOperations.set('myfunction', select  o from " + toCamelCase(table, true) + " where o.someField = :someValue");
            pw.println("    }");
            pw.println(" }");

            pw.println();

            pw.println("module.exports = function(metaData) {");
            pw.println("    return new " + f.getName().replace(".js", "") + "(metaData);");
            pw.println("};");

        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex) {
            };
        }
    }

    private File getModelFile(String table) {
        File retval = new File(config.getProperty("target.folder")
            + File.separator + "model" + File.separator + toCamelCase(table, true) + ".js");

        if (!retval.getParentFile().exists()) {
            retval.getParentFile().mkdirs();
        }

        return retval;
    }

    private File getMetaDataFile(String table) {
        File retval = new File(config.getProperty("target.folder")
            + File.separator + "metadata" + File.separator + toCamelCase(table, true) + "MetaData.js");

        if (!retval.getParentFile().exists()) {
            retval.getParentFile().mkdirs();
        }

        return retval;
    }

    private File getRepositoryFile(String table) {
        File retval = new File(config.getProperty("target.folder")
            + File.separator + "repository" + File.separator + toCamelCase(table, true) + "Repository.js");

        if (!retval.getParentFile().exists()) {
            retval.getParentFile().mkdirs();
        }

        return retval;
    }

    private String toCamelCase(String input, boolean upperCaseFirst) {
        StringBuilder retval = new StringBuilder();
        boolean hasUnderscore = input.contains("_");
        for (int i = 0; i < input.length(); ++i) {
            if (i == 0) {
                if (upperCaseFirst) {
                    retval.append(Character.toUpperCase(input.charAt(i)));
                } else {
                    retval.append(Character.toLowerCase(input.charAt(i)));
                }
            } else if (input.charAt(i) == '_') {
                i++;
                retval.append(Character.toUpperCase(input.charAt(i)));
            } else if (hasUnderscore) {
                retval.append(Character.toLowerCase(input.charAt(i)));
            } else {
                retval.append(input.charAt(i));
            }
        }

        return retval.toString();
    }

    private Connection getConnection() throws Exception {
        Class.forName(config.getProperty("db.driver"));
        return DriverManager.getConnection(config.getProperty("db.url"),
            config.getProperty("db.username"), config.getProperty("db.password"));
    }

    private boolean isNumeric(int sqlType) {
        switch (sqlType) {
            case java.sql.Types.BIGINT:
            case java.sql.Types.DECIMAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
            case java.sql.Types.SMALLINT:
                return true;
            default:
                return false;
        }
    }

    private boolean isNumericLengthValid(int sqlType) {
        switch (sqlType) {
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
                return true;
            default:
                return false;
        }
    }

    private boolean isLengthValid(int sqlType) {
        switch (sqlType) {
            case java.sql.Types.CHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.VARCHAR:
                return true;
            default:
                return false;
        }

    }

}
