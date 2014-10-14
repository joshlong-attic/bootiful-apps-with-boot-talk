package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/*
 * This Code Demonstrates:
 *
     * opinionated starters
     * Mongo
     * REST
     * HATEOAS
     * operations friendliness
     * deployment options
     * how auto-configuration works
     * customers using Boot
     * where to go from here
     *  - vaadin
     *  - activiti
     *  - spring cloud
 */

@Configuration
@ComponentScan
@EnableAutoConfiguration // @IWantToGoHomeEarly
public class Application {


    @Bean
    HealthIndicator healthIndicator() {
        return () -> Health.status("I <3 London!").build();
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    Facebook facebook(@Value("${facebook.appId}") String appId,
                      @Value("${facebook.appSecret}") String appSecret) {
        return new FacebookTemplate(appId + '|' + appSecret);
    }

    @Bean
    CommandLineRunner init(RestTemplate restTemplate,
                           Facebook facebook,
                           PlaceRepository placeRepository) {
        return args -> {

            String ip = restTemplate.getForObject("http://icanhazip.com", String.class).trim();
            LatLon loc = restTemplate.getForObject("http://freegeoip.net/json/{ip}", LatLon.class, ip);

            System.out.println("the IP of the current machine is " + ip);
            System.out.println("latLon:" + loc.toString());

            placeRepository.deleteAll();

            System.out.println("All records near current IP:");
            Arrays.asList("Starbucks", "Boots").forEach(q ->
                    facebook.placesOperations()
                            .search(q, loc.latitude, loc.longitude, 50000 - 1)
                            .stream()
                            .map(p -> placeRepository.save(new Place(p)))
                            .forEach(System.out::println));

            System.out.println("Lets zoom in..");
            placeRepository.findByPositionNear(
                    new Point(loc.longitude, loc.latitude),
                    new Distance(1, Metrics.MILES))
                    .forEach(System.out::println);
        };
    }

    static class LatLon {
        float longitude, latitude;

        public LatLon() {
        }

        public float getLongitude() {
            return longitude;
        }

        public float getLatitude() {
            return latitude;
        }

        @Override
        public String toString() {
            return "LatLon{" +
                    "longitude=" + longitude +
                    ", latitude=" + latitude +
                    '}';
        }

        public LatLon(float longitude, float latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


@Controller
class PlaceMvcController {

    @RequestMapping("/places.html")
        // ?city=Braintree
    String all(@RequestParam Optional<String> city, Model model) {
        List<Place> places = city.map(this.placeRepository::findByCity)
                .orElseGet(this.placeRepository::findAll);
        model.addAttribute("places", places);
        return "places"; //src/main/resources/templates +$VIEW +.html
    }

    @Autowired
    private PlaceRepository placeRepository;
}

@RestController
class GreetingsRestController {
    @RequestMapping("/hi/{name}")
    String hi(@PathVariable(value = "name") String name) {
        return "Hi " + name + "!";
    }
}

@RepositoryRestResource
interface PlaceRepository extends MongoRepository<Place, String> {

    List<Place> findByCity(String n);

    List<Place> findByPositionNear(org.springframework.data.geo.Point p,
                                   org.springframework.data.geo.Distance d);
}


@Document
class Place {

    @Id
    public String id;

    @GeoSpatialIndexed(name = "position")
    public double[] position;

    public double latitude;
    public double longitude;
    public Date insertionDate;
    public String city, country, description, state, street, zip, name, affilitation, category, about;

    public Place(Page p) {
        this.affilitation = p.getAffiliation();
        this.id = p.getId();
        this.name = p.getName();
        this.category = p.getCategory();
        this.description = p.getDescription();
        this.about = p.getAbout();
        this.insertionDate = new Date();
        org.springframework.social.facebook.api.Location pageLocation = p.getLocation();
        this.city = pageLocation.getCity();
        this.country = pageLocation.getCountry();
        this.description = pageLocation.getDescription();
        this.latitude = pageLocation.getLatitude();
        this.longitude = pageLocation.getLongitude();
        this.state = pageLocation.getState();
        this.street = pageLocation.getStreet();
        this.zip = pageLocation.getZip();
        this.position = new double[]{this.longitude, this.latitude};
    }

    Place() {
    }

    @Override
    public String toString() {
        return "Place{" +
                "id='" + id + '\'' +
                ", position=" + Arrays.toString(position) +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", insertionDate=" + insertionDate +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", description='" + description + '\'' +
                ", state='" + state + '\'' +
                ", street='" + street + '\'' +
                ", zip='" + zip + '\'' +
                ", name='" + name + '\'' +
                ", affilitation='" + affilitation + '\'' +
                ", category='" + category + '\'' +
                ", about='" + about + '\'' +
                '}';
    }
}
