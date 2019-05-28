package hu.zeletrik.wear.glanceface;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Comparator;
import java.util.Date;

public class CalendarEvent {
    private String title = "";
    private String location = "";
    private Date startDate;
    private Date endDate;
    private boolean isAllDay = false;
    private boolean isAttending = true;

    public static class EventComparator implements Comparator<CalendarEvent> {
        @Override
        public int compare(CalendarEvent event1, CalendarEvent event2) {
            return event1.getStartDate().compareTo(event2.getStartDate());
        }
    }


    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public boolean isAttending() {
        return isAttending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CalendarEvent that = (CalendarEvent) o;

        return new EqualsBuilder()
                .append(isAllDay, that.isAllDay)
                .append(isAttending, that.isAttending)
                .append(title, that.title)
                .append(location, that.location)
                .append(startDate, that.startDate)
                .append(endDate, that.endDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(title)
                .append(location)
                .append(startDate)
                .append(endDate)
                .append(isAllDay)
                .append(isAttending)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
                .append("title", title)
                .append("location", location)
                .append("startDate", startDate)
                .append("endDate", endDate)
                .append("isAllDay", isAllDay)
                .append("isAttending", isAttending)
                .toString();
    }

    public static CalendarEventBuilder builder() {
        return new CalendarEventBuilder();
    }

    public static final class CalendarEventBuilder {
        private String title = "";
        private String location = "";
        private Date startDate;
        private Date endDate;
        private boolean isAllDay = false;
        private boolean isAttending = true;

        private CalendarEventBuilder() {
        }

        public static CalendarEventBuilder aCalendarEvent() {
            return new CalendarEventBuilder();
        }

        public CalendarEventBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CalendarEventBuilder location(String location) {
            this.location = location;
            return this;
        }

        public CalendarEventBuilder startDate(Date startDate) {
            this.startDate = startDate;
            return this;
        }

        public CalendarEventBuilder endDate(Date endDate) {
            this.endDate = endDate;
            return this;
        }

        public CalendarEventBuilder isAllDay(boolean isAllDay) {
            this.isAllDay = isAllDay;
            return this;
        }

        public CalendarEventBuilder isAttending(boolean isAttending) {
            this.isAttending = isAttending;
            return this;
        }

        public CalendarEvent build() {
            CalendarEvent calendarEvent = new CalendarEvent();
            calendarEvent.isAllDay = this.isAllDay;
            calendarEvent.startDate = this.startDate;
            calendarEvent.endDate = this.endDate;
            calendarEvent.location = this.location;
            calendarEvent.isAttending = this.isAttending;
            calendarEvent.title = this.title;
            return calendarEvent;
        }
    }
}