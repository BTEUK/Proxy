package net.bteuk.proxy.sql.migration;

public record AcceptData(int id, String uuid, String reviewer, int bookId, int accuracy, int quality, long acceptTime) { }
