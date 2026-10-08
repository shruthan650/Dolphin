import Input from '../common/Input';

/** Required class picker for a student's own work (the backend checks they are enrolled). */
export default function ClassSelect({ classes, ...props }) {
  return (
    <Input as="select" label="Class" required hint="This work belongs to the selected class" {...props}>
      <option value="" disabled>
        Select a class
      </option>
      {classes.map((c) => (
        <option key={c.id} value={c.id}>
          {c.className}
        </option>
      ))}
    </Input>
  );
}
