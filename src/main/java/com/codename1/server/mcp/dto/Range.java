package com.codename1.server.mcp.dto;

public record Range(Pos start, Pos end) {
    public record Pos(int line, int col) {}
}
