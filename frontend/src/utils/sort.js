const collator = new Intl.Collator(undefined, { sensitivity: 'base', numeric: true });

function compareNullableStrings(left, right) {
  const leftValue = left == null ? '' : String(left).trim();
  const rightValue = right == null ? '' : String(right).trim();
  if (!leftValue && rightValue) return 1;
  if (leftValue && !rightValue) return -1;
  return collator.compare(leftValue, rightValue);
}

function compareIds(left, right) {
  return collator.compare(String(left ?? ''), String(right ?? ''));
}

export function sortByName(items, getName = (item) => item?.name, getTieBreaker = (item) => item?.id) {
  return [...(items ?? [])].sort((left, right) => {
    const nameOrder = compareNullableStrings(getName(left), getName(right));
    return nameOrder || compareIds(getTieBreaker(left), getTieBreaker(right));
  });
}

export function sortByDate(items, getDate, getTieBreaker = (item) => item?.id) {
  return [...(items ?? [])].sort((left, right) => {
    const leftTime = Date.parse(getDate(left) ?? '');
    const rightTime = Date.parse(getDate(right) ?? '');
    const leftValid = Number.isFinite(leftTime);
    const rightValid = Number.isFinite(rightTime);
    if (!leftValid && rightValid) return 1;
    if (leftValid && !rightValid) return -1;
    if (leftValid && rightValid && leftTime !== rightTime) return leftTime - rightTime;
    return compareIds(getTieBreaker(left), getTieBreaker(right));
  });
}

export function sortStrings(items) {
  return [...(items ?? [])].sort(compareNullableStrings);
}
