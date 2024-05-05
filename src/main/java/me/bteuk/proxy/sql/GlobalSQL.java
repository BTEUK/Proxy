package me.bteuk.proxy.sql;

import org.apache.commons.dbcp2.BasicDataSource;

public class GlobalSQL extends AbstractSQL {
    public GlobalSQL(BasicDataSource datasource) {
        super(datasource);
    }
}
