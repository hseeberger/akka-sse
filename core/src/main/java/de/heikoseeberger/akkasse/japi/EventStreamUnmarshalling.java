package de.heikoseeberger.akkasse.japi;

import akka.NotUsed;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.javadsl.Source;

/**
 * Using `eventStreamMarshaller` lets a source of [[ServerSentEvent]]s be marshalled to a `HttpResponse`.
 */
public abstract class EventStreamUnmarshalling {

    public static Unmarshaller<HttpEntity, Source<ServerSentEvent, NotUsed>> fromEventStream() {
        return Unmarshaller.fromScala(EventStreamUnmarshallingConverter$.MODULE$.fromEventStream());
    }
}
