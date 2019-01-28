package sonata.kernel.adaptor;


import org.glassfish.jersey.server.ResourceConfig;

public class JerseyConfiguration extends ResourceConfig {

    public JerseyConfiguration() {
        packages("sonata.kernel.adaptor.api");
    }
}
