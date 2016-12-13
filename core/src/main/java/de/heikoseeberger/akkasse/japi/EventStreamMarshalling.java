package de.heikoseeberger.akkasse.japi;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.javadsl.Source;

public class EventStreamMarshalling {

    public static <T> Marshaller<Source<ServerSentEvent, T>, HttpResponse> eventStreamMarshaller() {
        return Marshaller.fromScala(EventStreamMarshallingConverter$.MODULE$.jtrm());
    }
}
