
# API Gatekeeper Frontend

[![Build Status](https://travis-ci.org/hmrc/api-gatekeeper-frontend.svg?branch=master)](https://travis-ci.org/hmrc/api-gatekeeper-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gatekeeper-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/api-gatekeeper-frontend/_latestVersion)

This is a Play Framework frontend application for HMRC's internal users.
The application provides authentication and defines a role based access control for restricted actions. 

## Run the application

To run the application execute

```
sbt 'run 9000' 
```

and then access the application at

```
http://localhost:9000/api-gatekeeper/login
```

The application is using an authentication and authorisation service, it is not yet possible to stub them. 

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

