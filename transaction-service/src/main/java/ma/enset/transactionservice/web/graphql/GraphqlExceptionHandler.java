package ma.enset.transactionservice.web.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import ma.enset.transactionservice.service.exception.*;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphqlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType type;
        if (ex instanceof TransactionNotFoundException || ex instanceof AccountNotFoundException) {
            type = ErrorType.NOT_FOUND;
        } else if (ex instanceof InsufficientBalanceException || ex instanceof AccountNotActiveException
                || ex instanceof CurrencyMismatchException || ex instanceof InvalidAmountException
                || ex instanceof InvalidTransferException || ex instanceof IllegalArgumentException) {
            type = ErrorType.BAD_REQUEST;
        } else if (ex instanceof AccountServiceUnavailableException) {
            type = ErrorType.INTERNAL_ERROR;
        } else {
            return null;
        }
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }
}
