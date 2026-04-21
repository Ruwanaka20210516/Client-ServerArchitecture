package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory data store shared across singleton resource classes.
 * Using ConcurrentHashMap because the JAX-RS runtime may dispatch multiple
 * requests concurrently against the same store.
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seed();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private void seed() {
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("LAB-105", "Computing Lab", 30);
        rooms.put(lib.getId(), lib);
        rooms.put(lab.getId(), lab);

        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.4, "LIB-301");
        Sensor co2 = new Sensor("CO2-014", "CO2", "ACTIVE", 410.0, "LIB-301");
        Sensor occ = new Sensor("OCC-002", "Occupancy", "MAINTENANCE", 0.0, "LAB-105");

        sensors.put(temp.getId(), temp);
        sensors.put(co2.getId(), co2);
        sensors.put(occ.getId(), occ);

        lib.getSensorIds().add(temp.getId());
        lib.getSensorIds().add(co2.getId());
        lab.getSensorIds().add(occ.getId());

        readings.put(temp.getId(), new ArrayList<>());
        readings.put(co2.getId(), new ArrayList<>());
        readings.put(occ.getId(), new ArrayList<>());
    }

    // ---- Rooms ----
    public List<Room> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public void saveRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // ---- Sensors ----
    public List<Sensor> listSensors() {
        return new ArrayList<>(sensors.values());
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public void saveSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.computeIfAbsent(sensor.getId(), k -> new ArrayList<>());
    }

    public Sensor removeSensor(String id) {
        readings.remove(id);
        return sensors.remove(id);
    }

    // ---- Readings ----
    public List<SensorReading> listReadings(String sensorId) {
        List<SensorReading> list = readings.get(sensorId);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    public void appendReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(reading);
    }
}
