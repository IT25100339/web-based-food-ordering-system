package com.foodorderingsystem.admin.singletone;

// Classic GoF Singleton (matches the lecture's "private constructor + static getInstance()"
// structure exactly): there is only ever ONE AppSettings object for the whole running
// application, holding site-wide settings the admin controls - an announcement banner shown
// to every customer, and a maintenance-mode switch. Any class that needs these settings calls
// AppSettings.getInstance() rather than creating its own copy, so every part of the app (the
// admin dashboard that edits it, the customer home page that reads it) always sees the same values.
public class AppSettings {

    // the one and only instance - starts null, created the first time getInstance() is called
    private static AppSettings instance;

    private String announcementMessage = "";
    private boolean maintenanceMode = false;

    // private constructor: nothing outside this class can do "new AppSettings()"
    private AppSettings() {
    }

    public static synchronized AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
        }
        return instance;
    }

    public String getAnnouncementMessage() {
        return announcementMessage;
    }

    public void setAnnouncementMessage(String announcementMessage) {
        this.announcementMessage = announcementMessage == null ? "" : announcementMessage.trim();
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }
}
