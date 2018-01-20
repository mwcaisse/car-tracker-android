package com.ricex.cartracker.androidrequester.request.response;

import org.springframework.http.HttpStatus;

public abstract class RequestResponse<T> {
	
	/** The HttpStatus code of the response */
	private final HttpStatus statusCode;
	
	/** Creates a new instance of RequestResponse, representing a successful response
	 *
	 * @param statusCode The status code of the response
	 */
	public RequestResponse(HttpStatus statusCode) {

		this.statusCode = statusCode;
	}

	/** The response from the server if valid
	 * 
	 * @return The response from the server
	 */
	public abstract T getData();
	
	/** Returns the error received by the server, if invalid response
	 * 
	 * @return The error the server returned
	 */
	public abstract String getError();
	
	/** Return the status code received from the server
	 * 
	 * @return the status code received from the server
	 */
	public HttpStatus getStatusCode() {
		return statusCode;
	}	
	
	/** Returns whether the server response was okay or not
	 * 
	 * @return True if server responded without errors
	 */
	public boolean isValidServerResponse() {
		return HttpStatus.OK.equals(getStatusCode());
	}
	
	/** Returns whether this response is valid or not
	 * 
	 * @return True if response is valid, false otherwise
	 */
	public abstract boolean isValid();
	
	

}
