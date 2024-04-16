# [Cloud Computing] Assignment
## Making Web Server

### Project Structure
```agsl
- web-server
 - Main.java
 - servers
    - TCPServer.java
    - HTTPServer.java // extends TCPServer
 - models
    - HTTPRequest.java
    - HTTPResponse.java
 - handlers
    - ConnectionHandler.java // interface
    - HTTPConnectionHandler.java // implements ConnectionHandler
 - errors
    - BadRequestException.java
    - RecvTimeoutError.java
```

### Getting Started
1. Clone the repository
2. Run the Main.java file
3. Open the browser and go to `localhost:80`

### Features
- (Writting)