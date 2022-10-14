/**
 * The c.n.p.t3k package holds the base API package for talking with the T3K CORE server.
 * <p>
 *     This package centers around {@link com.nuix.proserv.t3k.T3KApi}: you use an instance of this class to communicate
 *     with the server, wether you want to {@link com.nuix.proserv.t3k.T3KApi#upload(long, java.lang.String)} an item
 *     to T3K, or {@link com.nuix.proserv.t3k.T3KApi#getResults(long)} for an item that was analyzed.
 * </p>
 * <p>
 *     An example of using this api would be:
 * </p>
 * <pre>
 *     T3KApi api = new T3KApi(uriToServer, serverPort, batchSize, retryCount, retryDelay);
 *     long itemId = api.upload(sourceId, pathToItem);
 *     PollResults poll = api.waitForAnalysis(itemId);
 *     if(poll.isError()) {
 *         // handle the error case
 *     }
 *     AnalysisResults results = api.getResults(itemId);
 * </pre>
 * <p>
 *     Results returned from the server are JSON strings which get de-serialized to specific results based on the
 *     request type and response body.  For example, the return from a poll() or waitForAnalysis() method will be
 *     deserialized to a {@link com.nuix.proserv.t3k.results.PollResults} object.  The result of a getResults() method
 *     will be a subclass of {@link com.nuix.proserv.t3k.results.AnalysisResult}, determined by the structure of the
 *     JSON returned by the server.
 * </p>
 * <p>
 *     This package also has the {@link com.nuix.proserv.t3k.T3KApiException} class, which is a runtime exception used
 *     to encapsulate all exceptions that occur when communicating with the server or parsing the results.
 * </p>
 * <p>
 *     There are two sub-packages:
 * </p>
 * <dl>
 *     <dt>c.n.p.t3k.results</dt>
 *     <dd>
 *         Contains the classes containing the deserialized results from the server, and related classes (like classes
 *         used to do the deserialization, and hold item metadata.
 *     </dd>
 *     <dt>c.n.p.t3k.detections</dt>
 *     <dd>
 *         Contains the classes containing the deserialized detections that get added to results, and related classes
 *         (like classes used to do the deserialization, and classes with deserialized extra data).
 *     </dd>
 * </dl>
 */
package com.nuix.proserv.t3k;