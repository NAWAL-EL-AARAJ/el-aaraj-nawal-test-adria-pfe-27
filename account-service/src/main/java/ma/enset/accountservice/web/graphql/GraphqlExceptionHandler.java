package ma.enset.accountservice.web.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import ma.enset.accountservice.service.exception.AccountBlockedException;
import ma.enset.accountservice.service.exception.AccountNotFoundException;
import ma.enset.accountservice.service.exception.InsufficientBalanceException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphqlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType type;
        if (ex instanceof AccountNotFoundException) {
            type = ErrorType.NOT_FOUND;
        } else if (ex instanceof InsufficientBalanceException || ex instanceof AccountBlockedException
                || ex instanceof IllegalArgumentException) {
            type = ErrorType.BAD_REQUEST;
        } else {
            return null;
        }
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }
}
