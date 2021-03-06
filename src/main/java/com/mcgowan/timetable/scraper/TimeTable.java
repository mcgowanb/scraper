package com.mcgowan.timetable.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class TimeTable implements Serializable {

    private String url, studentID, department, studentGroup, deptKey, groupKey;
    private Link link;
    private Document doc;
    private Map<String, List<Course>> days;
    private Map<String, String> dayNames;
    public static final String lineBreak = "==================================================";
    private String status;
    private boolean isValid;


    /**
     * Takes Student ID and URL for website. Returns object of weekly timetable
     * @param url
     * @param studentID
     * @throws IOException
     */
    public TimeTable(String url, String studentID) throws IOException {
        isValid = false;
        this.studentID = studentID;
        this.url = url;
        dayNames = new Day().getDayNames();
        doc = loadDataFromWeb(studentID);
    }


    public TimeTable(String url, String dept, String group) throws IOException{
        isValid = false;
        this.url = url;
        doc = loadDataFromWeb(dept, group);
    }


    public void process() throws IOException{
        dayNames = new Day().getDayNames();
        parseDaysFromDoc(doc);
        generateLink();
        department = new SelectedOption(doc, "#dept").toString();
        studentGroup = new SelectedOption(doc, "#studentgroup").toString();
        deptKey = new SelectedOption(doc, "#dept", true).toString();
        groupKey = new SelectedOption(doc, "#studentgroup", true).toString();
    }


    /**
     * Takes day name in string format and returns classes for that particular day
     *
     * @param day
     * @return
     */
    public List<Course> classesForDay(String day) {
        return days.get(day);
    }

    /**
     * returns link for the timetable site view
     */
    private void generateLink() {
        Element e = doc.select("div.tt_details > div.tt_detail > a").first();
        link = new Link(e);
    }

    /**
     * generates list of days and respective classes for each day
     *
     * @param doc
     */
    private void parseDaysFromDoc(Document doc) {
        Elements courseEls = doc.select("div.tt_details:not(:has(div.tt_day, a))");

        status = doc.select("section.entry-content > form").first().nextSibling().toString().trim();
        if (status.length() == 0) {
            isValid = true;
        }

        days = new LinkedHashMap<String, List<Course>>();
        for (Element courseEl : courseEls) {
            Element timeSlotEl = courseEl.select(".tt_timeslot").first();
            String timeSlotStr = timeSlotEl.ownText();
            String dayStr = timeSlotEl.select(".tt_day_small").first().text().trim().replace("(", "").replace(")", "");
            dayStr = dayNames.get(dayStr);
            String detailStr = courseEl.select(".tt_detail").first().text();
            String lecturerStr = courseEl.select(".tt_lecturer").first().text();

            Course course = new Course(dayStr, timeSlotStr, lecturerStr, detailStr);
            List<Course> courses = days.get(dayStr);

            if (courses == null) {
                courses = new ArrayList<Course>();
                days.put(dayStr, courses);
            }
            courses.add(course);
        }
    }

    /**
     * loads document from web
     *
     * @param studentID
     * @return
     * @throws IOException
     */
    private Document loadDataFromWeb(String studentID) throws IOException {
        doc = Jsoup.connect(url)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .data("dept", "")
                .data("student_id", studentID)
                .data("studentgroup", "")
                .data("view", "View Timetable")
                .post();
        return doc;
    }

    /**
     * loads document from web
     *
     * @return
     * @throws IOException
     */
    private Document loadDataFromWeb(String dept, String group) throws IOException {
        doc = Jsoup.connect(url)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .data("dept", dept)
                .data("studentgroup", group)
                .data("view", "View Timetable")
                .post();
        return doc;
    }

    @Override
    public String toString() {
        String output = "";
        if (isValid) {
            output += String.format("Student Number: %s\nDepartment: %s \nDepartment Key: %s\nStudent Group: %s\nGroup Key: %s\nTitle: %s\nURL: %s \n",
                    studentID, department, deptKey, studentGroup, groupKey, link.getTitle(), link.getLink());
            for (Map.Entry<String, List<Course>> entry : days.entrySet()) {
                output += entry.getKey() + "\n";
                output += lineBreak + "\n";
                for (Course c : entry.getValue()) {
                    output += "\t" + c + "\n";
                }
            }
        } else {
            output = status;
        }
        return output;
    }

    /**
     * returns selected item
     *
     * @param selector
     * @return
     */
    private String selectedTitle(String selector) {
        String title;
        try {
            title = doc.select("div." + selector + " > div select option[selected]").first().text();
        } catch (NullPointerException e) {
            title = "";
        }
        return title;
    }

    public Map<String, List<Course>> getDays() {
        return days;
    }

    public String getDepartment() {
        return department;
    }

    public String getStudentGroup() {
        return studentGroup;
    }

    public Link getLink() {
        return link;
    }

    public String getStatus() {
        return status;
    }

    public String getDeptKey() {
        return deptKey;
    }

    public String getGroupKey() {
        return groupKey;
    }
}
