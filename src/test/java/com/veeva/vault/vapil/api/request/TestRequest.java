/*---------------------------------------------------------------------
 *	Copyright (c) 2021 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.vapil.api.request;

import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.connector.HttpRequestConnector;
import com.veeva.vault.vapil.connector.HttpRequestConnector.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRequest extends VaultRequest<TestRequest> {
	private static Logger log = LoggerFactory.getLogger(TestRequest.class);

	// API Endpoints
	private static final String URL_TEST = "v24.3/custom/test";

	private TestRequest() {
	}

	/**
	 * <b>Test - Custom WebAPI</b>
	 * <p>
	 * Execute Test API
	 *
	 * @return VaultResponse
	 * @vapil.api <pre>
	 * POST /api/{version}/custom/test</pre>
	 * @vapil.request <pre>
	 * VaultResponse response = vaultClient.newRequest(TestRequest.class).test();
	 * </pre>
	 */
	public VaultResponse test(String type, int iterations) {
		String url = vaultClient.getAPIEndpoint(URL_TEST, false);

		HttpRequestConnector request = new HttpRequestConnector(url);
		request.addBodyParamMultiPart("type", type);
		request.addBodyParamMultiPart("iterations", iterations);
		return send(HttpMethod.POST, request, VaultResponse.class);
	}
}
