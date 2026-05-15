package dev.nthings.helm4j.release;

/** Result of `helm get notes`. */
public record GetNotesResult(String notes) {}
