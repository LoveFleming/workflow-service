export default function TabButton({ children, isSelected, ...props }) {
  console.log("TabButton executing here");
  return (
    <li>
      <button className={isSelected ? "active" : undefined} {...props}>
        {children}
      </button>
    </li>
  );
}
