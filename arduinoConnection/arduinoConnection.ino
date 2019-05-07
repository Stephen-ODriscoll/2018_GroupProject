#define CUSTOM_SETTINGS
#define INCLUDE_INTERNET_SHIELD
#define INCLUDE_GPS_SHIELD
#define INCLUDE_TERMINAL_SHIELD

#include <OneSheeld.h>

HttpRequest latitudeRequest("https://webapp-5454.firebaseio.com/cars/car1/Latitude.json");
HttpRequest longitudeRequest("https://webapp-5454.firebaseio.com/cars/car1/Longitude.json");
 
void setup() {
  // put your setup code here, to run once:
  OneSheeld.begin();

  latitudeRequest.setOnSuccess(&onSuccess);
  latitudeRequest.getResponse().setOnError(&onResponseError);
  
  Internet.setOnError(&onInternetError);
}
 
void loop() {
  // put your main code here, to run repeatedly:
  latitudeRequest.addRawData(dtostrf(GPS.getLatitude(), 11, 6, "           "));
  longitudeRequest.addRawData(dtostrf(GPS.getLongitude(), 11, 6, "           "));
  Internet.performPut(latitudeRequest);
  Internet.performPut(longitudeRequest);
  OneSheeld.delay(1000);
}

void onSuccess(HttpResponse & response)
{
  Terminal.println("Succeeded");
}

/* Error handling functions. */
void onResponseError(int errorNumber)
{
  /* Print out error Number.*/
  Terminal.print("Response error:");
  switch(errorNumber)
  {
    case INDEX_OUT_OF_BOUNDS: Terminal.println("INDEX_OUT_OF_BOUNDS");break;
    case RESPONSE_CAN_NOT_BE_FOUND: Terminal.println("RESPONSE_CAN_NOT_BE_FOUND");break;
    case HEADER_CAN_NOT_BE_FOUND: Terminal.println("HEADER_CAN_NOT_BE_FOUND");break;
    case NO_ENOUGH_BYTES: Terminal.println("NO_ENOUGH_BYTES");break;
    case REQUEST_HAS_NO_RESPONSE: Terminal.println("REQUEST_HAS_NO_RESPONSE");break;
    case SIZE_OF_REQUEST_CAN_NOT_BE_ZERO: Terminal.println("SIZE_OF_REQUEST_CAN_NOT_BE_ZERO");break;
    case UNSUPPORTED_HTTP_ENTITY: Terminal.println("UNSUPPORTED_HTTP_ENTITY");break;
    case JSON_KEYCHAIN_IS_WRONG: Terminal.println("JSON_KEYCHAIN_IS_WRONG");break;
  }
}
void onInternetError(int requestId, int errorNumber)
{
  /* Print out error Number.*/
  Terminal.print("Request id:");
  Terminal.println(requestId);
  Terminal.print("Internet error:");
  switch(errorNumber)
  {
    case REQUEST_CAN_NOT_BE_FOUND: Terminal.println("REQUEST_CAN_NOT_BE_FOUND");break;
    case NOT_CONNECTED_TO_NETWORK: Terminal.println("NOT_CONNECTED_TO_NETWORK");break;
    case URL_IS_NOT_FOUND: Terminal.println("URL_IS_NOT_FOUND");break;
    case ALREADY_EXECUTING_REQUEST: Terminal.println("ALREADY_EXECUTING_REQUEST");break;
    case URL_IS_WRONG: Terminal.println("URL_IS_WRONG");break;
  }
}
