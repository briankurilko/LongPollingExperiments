import React, { useState } from "react";
import TalkForm from "./components/TalkForm";
import Talks from "./components/Talks";
import "./styles/styles.scss";

export default function App() {
  const [name, setName] = useState(localStorage.getItem("userName") || "Anon");
  return (
    <div className="App">
      <h1>Skill Sharing</h1>
      Your name:
      <br />
      <input
        type="text"
        onChange={(event) => {
          setName(event.target.value);
          localStorage.setItem("userName", event.target.value);
        }}
        value={name}
      />
      <Talks name={name} />
      <TalkForm name={name} />
    </div>
  );
}
