# 🌐 How the Frontend is Served

## 📍 Current Setup

Your frontend files are located in:
```
src/main/resources/static/
├── index.html
├── script.js
└── style.css
```

### How Spring Boot Serves Static Files

Spring Boot **automatically serves** static files from these locations (in order of priority):

1. `/src/main/resources/static/`
2. `/src/main/resources/public/`
3. `/src/main/resources/resources/`
4. `/src/main/resources/META-INF/resources/`

**Your files are in the default location!** ✅

---

## 🚀 How to Access Your Frontend

### When the Application is Running

Your frontend is served at:

```
http://localhost:8080/
http://localhost:8080/index.html
```

**Spring Boot Magic:**
- `index.html` is served as the **default page** at `/`
- `script.js` is accessible at `/script.js`
- `style.css` is accessible at `/style.css`

### URL Mapping

| File Path | Served At | Example |
|-----------|-----------|---------|
| `static/index.html` | `http://localhost:8080/` | Root page |
| `static/script.js` | `http://localhost:8080/script.js` | JavaScript |
| `static/style.css` | `http://localhost:8080/style.css` | CSS |

---

## 🔄 How It Works

### 1. **Maven Build Process**

When you build your project:
```bash
mvn clean package
```

**What happens:**
```
src/main/resources/static/
    ↓
[Maven copies to]
    ↓
target/classes/static/
    ↓
[Packaged into]
    ↓
spring-rag-0.0.1-SNAPSHOT.war
    ├── WEB-INF/
    │   └── classes/
    │       └── static/       ← Your frontend files here
    │           ├── index.html
    │           ├── script.js
    │           └── style.css
    └── ...
```

### 2. **Spring Boot's Static Resource Handler**

Spring Boot automatically configures:
```java
// This happens automatically - you don't need to write this!
@Configuration
public class AutoConfiguredWebMvc {
    
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

**Translation:**
- Pattern `/**` = Match any URL path
- Location `classpath:/static/` = Serve from `src/main/resources/static/`

### 3. **Request Flow**

```
User opens browser: http://localhost:8080/
    ↓
Spring Boot checks: Is this an API endpoint?
    ↓
No, not under /api/**
    ↓
Check static resources in /static/
    ↓
Found: index.html
    ↓
Serve to browser
    ↓
Browser sees: <script src="script.js">
    ↓
Browser requests: http://localhost:8080/script.js
    ↓
Spring Boot serves: static/script.js
    ↓
✅ Frontend fully loaded!
```

---

## 🏗️ Project Structure

### Current Structure (Recommended)
```
Spring-AI-Topic-RAG/
├── frontend/                    ← Empty (can be removed)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/spring_rag/
│       │       ├── config/
│       │       ├── controller/   ← API endpoints (/api/*)
│       │       └── ...
│       └── resources/
│           ├── static/           ← 🌟 Frontend files here!
│           │   ├── index.html
│           │   ├── script.js
│           │   └── style.css
│           └── application.yaml
└── pom.xml
```

---

## 🎯 Why This Works

### Separation of Concerns

| Type | Path | Purpose |
|------|------|---------|
| **API Endpoints** | `/api/**` | REST API for backend logic |
| **Static Files** | `/**` (everything else) | Frontend HTML/CSS/JS |

### Example:
- `GET http://localhost:8080/api/health` → HealthController
- `GET http://localhost:8080/api/rag/topics` → TopicRagController
- `GET http://localhost:8080/` → `static/index.html`
- `GET http://localhost:8080/script.js` → `static/script.js`

---

## 🛠️ Development Workflow

### Making Changes to Frontend

1. **Edit files in:** `src/main/resources/static/`
2. **Spring Boot DevTools will auto-reload** (if running)
3. **Refresh browser** to see changes

### If DevTools Doesn't Pick Up Changes:

```bash
# Rebuild and restart
mvn clean compile
# Then restart your Spring Boot app
```

---

## 📦 Production Deployment

### As WAR File

Your project is packaged as a **WAR file**:
```bash
mvn clean package
# Creates: target/spring-rag-0.0.1-SNAPSHOT.war
```

**Everything is bundled together:**
- ✅ Java backend code
- ✅ Frontend files (from `static/`)
- ✅ Dependencies

**Deploy to:**
- Tomcat
- Jetty
- Any Servlet container

**Access:**
```
http://your-server:8080/           ← Frontend
http://your-server:8080/api/health ← Backend API
```

---

## 🔧 Advanced Configuration (Optional)

### Custom Static Resource Location

If you want to serve from a different location, update **WebConfig.java**:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve from custom location
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("file:./frontend/");  // External folder
    }
    
    // ...existing CORS config...
}
```

### Serve Different Index

If you want a custom welcome page:

```yaml
# application.yaml
spring:
  web:
    resources:
      static-locations: classpath:/static/
  mvc:
    static-path-pattern: /**
```

---

## 🐛 Troubleshooting

### Problem: "404 Not Found" when accessing root

**Check:**
1. Is the file in `src/main/resources/static/`?
2. Did you rebuild after adding files?
3. Is the application running on port 8080?

**Solution:**
```bash
mvn clean compile
# Restart Spring Boot
```

### Problem: "Changes not reflecting in browser"

**Causes:**
1. Browser cache
2. DevTools not reloading
3. Files not in correct location

**Solution:**
```bash
# Hard refresh browser: Ctrl+F5 (Windows/Linux) or Cmd+Shift+R (Mac)

# Or clear build and rebuild:
mvn clean compile
```

### Problem: "CORS errors in browser console"

**Already configured!** Your `WebConfig.java` allows all origins for API calls:
```java
.allowedOrigins("*")
```

---

## 📊 Request Priority Order

When a request comes in, Spring checks in this order:

```
1. @Controller / @RestController endpoints
   └─ Example: /api/health

2. Static resources in /static/
   └─ Example: /index.html, /script.js

3. 404 Not Found
```

**Your setup:**
- ✅ API endpoints: `/api/**` → Controllers
- ✅ Frontend: `/`, `/*.html`, `/*.js`, `/*.css` → Static files
- ✅ Clean separation!

---

## 🎉 Summary

### How Your Frontend is Delivered:

1. **Files stored in:** `src/main/resources/static/`
2. **Spring Boot automatically serves them** at the root path
3. **No additional configuration needed** ✨
4. **Access at:** `http://localhost:8080/`
5. **Packaged together** in the WAR file for deployment

### Your Current Setup is Perfect! ✅

- Backend API: `/api/**`
- Frontend: `/`
- Everything in one deployable WAR file
- No separate web server needed

Just start your Spring Boot application and open `http://localhost:8080/` in your browser! 🚀

