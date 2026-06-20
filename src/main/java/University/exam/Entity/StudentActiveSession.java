package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_active_sessions")
public class StudentActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "browser_info")
    private String browserInfo;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public StudentActiveSession() {}

    public StudentActiveSession(String studentId, String sessionId) {
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.status = "ACTIVE";
        this.isActive = true;
    }

    public StudentActiveSession(String studentId, String sessionId, String ipAddress, String browserInfo, String deviceInfo) {
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.browserInfo = browserInfo;
        this.deviceInfo = deviceInfo;
        this.status = "ACTIVE";
        this.isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }
    public void setStatus(String status) { 
        this.status = status; 
        this.isActive = "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        isActive = active; 
        this.status = active ? "ACTIVE" : "COMPLETED";
    }
}
