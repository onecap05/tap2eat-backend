from datetime import date, datetime, time


def resolve_report_date_range(
    from_date: date | None,
    to_date: date | None
) -> tuple[date, date, str, str]:
    if from_date is None and to_date is None:
        today = date.today()
        from_date = today
        to_date = today
    elif from_date is None:
        from_date = to_date
    elif to_date is None:
        to_date = from_date

    if from_date is None or to_date is None:
        raise ValueError("Both dates are required.")

    if from_date > to_date:
        raise ValueError("The start date cannot be after the end date.")

    from_datetime = datetime.combine(from_date, time.min).isoformat()
    to_datetime = datetime.combine(to_date, time.max).isoformat()

    return from_date, to_date, from_datetime, to_datetime