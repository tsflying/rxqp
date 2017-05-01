package com.ddz.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MainController {

	@RequestMapping(value = "/index")
	public ModelAndView index(HttpServletRequest request) {
		ModelAndView modelView = new ModelAndView("index");
		return modelView;
	}

	@RequestMapping(value = "/webSocket")
	public ModelAndView WebSocket() {
		ModelAndView mv = new ModelAndView("webSocket");
		return mv;
	}
}
