# Vaadin14 InputMask add-on

Add-on that creates custom-text-field with supporting InputMask and Swing Mask

## Development instructions

### Important Files 
It's needed to put file jquery-loader.js to folder in your project /frontend/src/ 
and file custom-textfield.css to folder in your project`` 


### Deployment

Starting the test/demo server:
```
mvn jetty:run
```

This deploys demo at http://localhost:8080
 
### Integration test

To run Integration Tests, execute `mvn verify -Pit,production`.
