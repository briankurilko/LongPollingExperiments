import React, { useState } from "react";
import axios from "axios";

export default function TalkForm({ name }) {
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");

  function onTalkFormSubmit(event) {
    event.preventDefault();
    axios
      .put(`/talks/${title}`, {
        presenter: name,
        summary,
      })
      .catch((error) => alert(error));
    event.target.reset();
  }

  return (
    <form onSubmit={onTalkFormSubmit}>
      <h3>Submit a talk</h3>
      <label>
        Title:
        <br />
        <input type="text" onChange={(event) => setTitle(event.target.value)} />
      </label>
      <label>
        Summary:
        <input
          type="text"
          onChange={(event) => setSummary(event.target.value)}
        />
      </label>
      <button type="submit">Submit</button>
    </form>
  );
}
