package net.bteuk.proxy.sql.migration;

public record DenyData(int id, String uuid, String reviewer, int bookId, int attempt, long denyTime) { }

