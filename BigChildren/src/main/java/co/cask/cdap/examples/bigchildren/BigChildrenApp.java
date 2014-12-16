/*
 * Copyright © 2014 Cask Data, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.examples.bigchildren;
import co.cask.cdap.api.annotation.Handle;
import co.cask.cdap.api.annotation.ProcessInput;
import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.dataset.lib.ObjectStore;
import co.cask.cdap.api.dataset.lib.ObjectStores;
import co.cask.cdap.api.flow.Flow;
import co.cask.cdap.api.flow.FlowSpecification;
import co.cask.cdap.api.flow.flowlet.AbstractFlowlet;
import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.api.procedure.AbstractProcedure;
import co.cask.cdap.api.procedure.ProcedureRequest;
import co.cask.cdap.api.procedure.ProcedureResponder;
import co.cask.cdap.api.procedure.ProcedureResponse;
import co.cask.cdap.api.spark.AbstractSpark;
import co.cask.cdap.api.spark.SparkSpecification;
import co.cask.cdap.internal.io.UnsupportedTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Application to find Correlations between child mortality and outlierness of weather data
 */
public class BigChildrenApp extends AbstractApplication {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public void configure() {
        setName("BigChildren");
        setDescription("BigChildren app");
        addStream(new Stream("weatherStream"));
        addStream(new Stream("childmortalityStream"));
        addFlow(new WeatherFlow());
        addFlow(new ChildMortalityFlow());
        addSpark(new BigChildrenSpecification());
        addProcedure(new CorrelationsProcedure());

        try {
            ObjectStores.createObjectStore(getConfigurer(), "Weather", String.class);
            ObjectStores.createObjectStore(getConfigurer(), "ChildMortality", String.class);
            ObjectStores.createObjectStore(getConfigurer(), "Correlations", String.class);
        } catch (UnsupportedTypeException e) {
            // This exception is thrown by ObjectStore if its parameter type cannot be
            // (de)serialized (for example, if it is an interface and not a class, then there is
            // no auto-magic way deserialize an object.) In this case that will not happen
            // because String is an actual class.
            throw new RuntimeException(e);
        }
    }

    /**
     * A Spark Program that uses BigData algorithms to find Correlations.
     */
    public static class BigChildrenSpecification extends AbstractSpark {
        @Override
        public SparkSpecification configure() {
            return SparkSpecification.Builder.with()
                .setName("BigChildrenProgram")
                .setDescription("BigChildren Program")
                .setMainClassName(BigChildrenProgram.class.getName())
                .build();
        }
    }

    /**
     * This Flowlet reads events from a Stream and saves them to a datastore.
     */
    public static class WeatherReader extends AbstractFlowlet {

        private static final Logger LOG = LoggerFactory.getLogger(WeatherReader.class);

        @UseDataSet("Weather")
        private ObjectStore<String> pointsStore;

        @ProcessInput
        public void process(StreamEvent event) {
            String body = new String(event.getBody().array());
            LOG.trace("Weather info: {}", body);
            pointsStore.write(getIdAsByte(UUID.randomUUID()), body);
        }

        private static byte[] getIdAsByte(UUID uuid) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }
    }

    /**
     * This Flowlet reads events from a Stream and saves them to a datastore.
     */
    public static class ChildMortalityReader extends AbstractFlowlet {
        private static final Logger LOG = LoggerFactory.getLogger(WeatherReader.class);

        @UseDataSet("ChildMortality")
        private ObjectStore<String> pointsStore;

        @ProcessInput
        public void process(StreamEvent event) {
            String body = new String(event.getBody().array());
            LOG.trace("Weather info: {}", body);
            pointsStore.write(getIdAsByte(UUID.randomUUID()), body);
        }

        private static byte[] getIdAsByte(UUID uuid) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }
    }

    /**
     * Flow: consumes weather data from a Stream, stores them in a dataset
     */
    public static class WeatherFlow implements Flow {
        @Override
        public FlowSpecification configure() {
            return FlowSpecification.Builder.with()
                .setName("WeatherFlow")
                .setDescription("Reads weather information for a given weather station and stores it in dataset")
                .withFlowlets()
                .add("reader", new WeatherReader())
                .connect()
                .fromStream("weatherStream").to("reader")
                .build();
        }
    }

    /**
     * Flow: consumes childmortality data from a Stream, stores them in a dataset
     */
    public static class ChildMortalityFlow implements Flow {
        @Override
        public FlowSpecification configure() {
            return FlowSpecification.Builder.with()
                .setName("ChildMortalityFlow")
                .setDescription("Reads child mortality information for a given weather station and stores it in dataset")
                .withFlowlets()
                .add("reader", new ChildMortalityReader())
                .connect()
                .fromStream("childmortalityStream").to("reader")
                .build();
        }
    }

    /**
     * returns calculated Correlation based on index parameter.
     */
    public static class CorrelationsProcedure extends AbstractProcedure {
        private static final Logger LOG = LoggerFactory.getLogger(CorrelationsProcedure.class);
        // Annotation indicates that Correlations dataset is used in the procedure.
        @UseDataSet("Correlations")
        private ObjectStore<String> Correlations;

        @Handle("Correlations")
        public void getCorrelations(ProcedureRequest request, ProcedureResponder responder)
            throws IOException, InterruptedException {
            String index = request.getArgument("index");
            if (index == null) {
                responder.error(ProcedureResponse.Code.CLIENT_ERROR, "Index must be given as argument");
                return;
            }
            LOG.debug("get Correlations for index {}", index);
            // Send response with JSON format.
            //responder.sendJson(Correlations.read(index.getBytes()));
            String result = Correlations.read(index.getBytes());
            if (result == null){
                responder.sendJson(ProcedureResponse.Code.SUCCESS, Correlations.scan(("0").getBytes(), ("1").getBytes()));
                // "this method returned null"
            } else {
                responder.sendJson(ProcedureResponse.Code.SUCCESS, result);
            }
        }
    }
}
