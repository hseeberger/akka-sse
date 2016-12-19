package de.heikoseeberger.akkasse.japi;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.RequestEntity;
import akka.stream.javadsl.Source;

/**
 * Using `eventStreamMarshaller` lets a source of [[ServerSentEvent]]s be marshalled to a `HttpResponse`.
 */
public abstract class EventStreamMarshalling {

    public static <T> Marshaller<Source<ServerSentEvent, T>, RequestEntity> toEventStream() {
        return Marshaller.fromScala(EventStreamMarshallingConverter$.MODULE$.toEventStream());
    }
}
