package de.heikoseeberger.akkasse.japi;

import java.util.Optional;
import java.util.OptionalInt;

import static de.heikoseeberger.akkasse.OptionConverters.*;

public abstract class ServerSentEvent {

    public abstract Optional<String> getData();

    public abstract Optional<String> getType();

    public abstract Optional<String> getId();

    public abstract OptionalInt getRetry();

    public static ServerSentEvent create(String data) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data);
    }

    public static ServerSentEvent create(int retry) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(retry);
    }

    public static ServerSentEvent create(String data, String type) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data, type);
    }

    public static ServerSentEvent create(String data, String type, String id) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data, type, id);
    }

    public static ServerSentEvent create(Optional<String> data,
                                         Optional<String> type,
                                         Optional<String> id,
                                         OptionalInt retry) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(
                toOption(data),
                toOption(type),
                toOption(id),
                toOption(retry)
        );
    }
}
