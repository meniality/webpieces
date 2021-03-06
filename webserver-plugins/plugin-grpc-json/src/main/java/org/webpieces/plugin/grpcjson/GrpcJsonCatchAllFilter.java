package org.webpieces.plugin.grpcjson;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.grpc.ErrorResponse;
import org.webpieces.grpc.ErrorResponse.Builder;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.router.api.controller.actions.Action;
import org.webpieces.router.api.controller.actions.RenderContent;
import org.webpieces.router.api.exceptions.AuthenticationException;
import org.webpieces.router.api.exceptions.AuthorizationException;
import org.webpieces.router.api.exceptions.ClientDataError;
import org.webpieces.router.api.exceptions.NotFoundException;
import org.webpieces.router.api.routes.MethodMeta;
import org.webpieces.router.api.routes.RouteFilter;
import org.webpieces.router.impl.compression.MimeTypes.MimeTypeResult;
import org.webpieces.util.filters.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

public class GrpcJsonCatchAllFilter extends RouteFilter<JsonConfig> {

	private static final Logger log = LoggerFactory.getLogger(GrpcJsonCatchAllFilter.class);
	public static final MimeTypeResult MIME_TYPE = new MimeTypeResult("application/json", StandardCharsets.UTF_8);

	private Boolean isNotFoundFilter;
	private Pattern pattern;

	public GrpcJsonCatchAllFilter() {
	}
	
	@Override
	public CompletableFuture<Action> filter(MethodMeta meta, Service<MethodMeta, Action> nextFilter) {
		if(isNotFoundFilter)
			return createNotFoundResponse(nextFilter, meta);

		return nextFilter.invoke(meta).handle((a, t) -> translateFailure(a, t));
	}

	@Override
	public void initialize(JsonConfig config) {
		this.isNotFoundFilter = config.isNotFoundFilter();
		this.pattern = config.getFilterPattern();
	}

	protected Action translateFailure(Action action, Throwable t) {
		if(t != null) {
			if(t instanceof ClientDataError) {
				return translate((ClientDataError) t);
			} else if(t instanceof AuthorizationException) {
				return translate((AuthorizationException)t);
			} else if(t instanceof AuthenticationException) {
				return translate((AuthenticationException) t);
			} else if (t instanceof NotFoundException) {
				return createNotFound();
			}
			
			log.error("Internal Server Error", t);
			return translateError(t);
		} else {
			return action;
		}
	}

	protected Action translate(AuthorizationException t) {
		byte[] content = translateAuthorizationError(t);
		KnownStatusCode status = KnownStatusCode.HTTP_401_UNAUTHORIZED;
		return new RenderContent(content, status.getCode(), status.getReason(), MIME_TYPE);
	}

	protected Action translate(AuthenticationException t) {
		byte[] content = translateAuthenticationError(t);
		KnownStatusCode status = KnownStatusCode.HTTP_403_FORBIDDEN;
		return new RenderContent(content, status.getCode(), status.getReason(), MIME_TYPE);
	}

	protected RenderContent translateError(Throwable t) {
		byte[] content = translateServerError(t);
		KnownStatusCode status = KnownStatusCode.HTTP_500_INTERNAL_SVR_ERROR;
		return new RenderContent(content, status.getCode(), status.getReason(), MIME_TYPE);
	}

	protected RenderContent translate(ClientDataError t) {
		byte[] content = translateClientError(t);
		KnownStatusCode status = KnownStatusCode.HTTP_400_BADREQUEST;
		return new RenderContent(content, status.getCode(), status.getReason(), MIME_TYPE);
	}

	protected CompletableFuture<Action> createNotFoundResponse(Service<MethodMeta, Action> nextFilter, MethodMeta meta) {
		Matcher matcher = pattern.matcher(meta.getCtx().getRequest().relativePath);
		if(!matcher.matches())
			return nextFilter.invoke(meta);
		
		return CompletableFuture.completedFuture(
					createNotFound()
				);
	}

	protected Action createNotFound() {
		byte[] content = createNotFoundJsonResponse();		
		return new RenderContent(content, KnownStatusCode.HTTP_404_NOTFOUND.getCode(), KnownStatusCode.HTTP_404_NOTFOUND.getReason(), MIME_TYPE);
	}

	protected byte[] translateAuthenticationError(AuthenticationException t) {
		Builder builder = ErrorResponse.newBuilder();
		builder.setError("403 Forbidden: "+t.getMessage());
		builder.setCode(403);

		return translateJson(builder);
	}

	protected byte[] translateAuthorizationError(AuthorizationException t) {
		Builder builder = ErrorResponse.newBuilder();
		builder.setError("401 Not Authorized : "+t.getMessage());
		builder.setCode(401);

		return translateJson(builder);
	}

	protected byte[] translateClientError(ClientDataError t) {
		Builder builder = ErrorResponse.newBuilder();
		builder.setError("400 bad request: "+t.getMessage());
		builder.setCode(400);

		return translateJson(builder);
	}

	protected byte[] createNotFoundJsonResponse() {
		Builder builder = ErrorResponse.newBuilder();
		builder.setError("404 This url does not exist.  try another url");
		builder.setCode(404);
		return translateJson(builder);
	}

	protected byte[] translateServerError(Throwable t) {
		Builder builder = ErrorResponse.newBuilder();
		builder.setError("Server ran into a bug, please report");
		builder.setCode(500);
		return translateJson(builder);
	}
	
	protected byte[] translateJson(MessageOrBuilder msg) {
		try {
            JsonFormat.Printer jsonPrinter = JsonFormat.printer();
            String json = jsonPrinter.print(msg);
            byte[] reqAsBytes = json.getBytes(Charset.forName("UTF-8"));
            return reqAsBytes;
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException("invalid message="+msg.getClass().getSimpleName(), e);
		}
	}
	
}
