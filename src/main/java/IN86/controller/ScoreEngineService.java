package IN86.controller;

import IN86.main.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/score")
public class ScoreEngineService {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @RequestMapping("/{component}")
    public void getScoreForComponent(@PathVariable String component){
        log.info("Getting score for " + component);
    }

    @RequestMapping
    public void getAllScores(){
        log.info("Getting all scores");
    }

}
