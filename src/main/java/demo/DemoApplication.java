package demo;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.jmx.JmxMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.cache.annotation.CacheResult;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

@SpringBootApplication
@EnableCaching
public class DemoApplication {

    @Service
    public static class CustomerService {

        @Autowired
        private CustomerRepository customerRepository;

        @CacheResult
        public Customer byId(Long id) throws Exception {
            Customer c = this.customerRepository.findOne(id);
            Thread.sleep(1000 * 5);
            return c;
        }
    }


    @RestController
    @RequestMapping("/customers")
    public static class CustomerRestController {

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private CustomerService customerService;

        @RequestMapping("/{id}")
        Customer byId(@PathVariable Long id) throws Exception {
            return this.customerService.byId(id);
        }

        @RequestMapping("/")
        Collection<Customer> customers() {
            return this.customerRepository.findAll();
        }
    }


    @Controller
    public static class CustomerMvcController {

        @Autowired
        private CustomerRepository customerRepository;

        @RequestMapping("/customers.php")
        String customers(Model model) {
            model.addAttribute("customers", this.customerRepository.findAll());
            return "customers";
        }
    }

    @Named
    @Path("/customers")
    @Produces({MediaType.APPLICATION_JSON})
    public static class ReservationEndpoint {

        @GET
        public Collection<Customer> customers() {
            return this.customerRepository.findAll();
        }

        @Inject
        private CustomerRepository customerRepository;

    }

    @Named
    @ApplicationPath(value = "/jersey")
    public static class JerseyConfig extends ResourceConfig {

        public JerseyConfig() {
            this.register(JerseyConfig.class);
            this.register(ReservationEndpoint.class);
            this.register(JacksonFeature.class);
        }
    }


    @Theme("valo")
    @SpringUI(path = "/ui")
    public static class CustomerUI extends UI {

        @Autowired
        private CustomerRepository customerRepository;

        @Override
        protected void init(VaadinRequest request) {

            Container container = new BeanItemContainer<Customer>(
                    Customer.class, this.customerRepository.findAll());

            Table table = new Table();
            table.setContainerDataSource(container);
            table.setSizeFull();

            setContent(table);
        }
    }

    @Configuration
    public static class ApplicationConfiguration {

        @Bean
        CommandLineRunner run(CustomerRepository repository) {
            return args
                    -> Arrays.asList("George,Peter,Henri,Sami,Joonas,Petter".split(","))
                    .forEach(x -> repository.save(new Customer(x)));
        }

        // this could be statsd, opentsdb, jmx, redis
        @Bean
        @ExportMetricWriter
        MetricWriter jmx(
                @Qualifier("mbeanExporter") MBeanExporter exporter) {
            return new JmxMetricWriter(exporter);
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

interface CustomerRepository extends JpaRepository<Customer, Long> {
}