package com.naveen.transitflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_bus_number", columnNames = {"bus_id", "seat_number"}))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    // "row_number" is reserved in MySQL 8, so the column is named seat_row
    @Column(name = "seat_row", nullable = false)
    private int rowNumber;

    @Column(name = "seat_col", nullable = false)
    private int columnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    protected Seat() {
    }

    public Seat(String seatNumber, int rowNumber, int columnNumber, SeatType seatType, Bus bus) {
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
        this.seatType = seatType;
        this.bus = bus;
    }

    public Long getId() { return id; }
    public String getSeatNumber() { return seatNumber; }
    public int getRowNumber() { return rowNumber; }
    public int getColumnNumber() { return columnNumber; }
    public SeatType getSeatType() { return seatType; }
    public Bus getBus() { return bus; }
}