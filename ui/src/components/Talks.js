import React, { useState, useEffect } from "react";
import axios from "axios";

export default function Talks({ name }) {
  const [talks, setTalks] = useState([]);

  function deleteTalk(title) {
    axios.delete(`/talks/${title}`).catch((error) => alert(error));
  }

  function submitComment(event, title) {
    event.preventDefault();
    const form = event.target;
    axios.post(`/talks/${title}/comments`, {
      author: name,
      message: form.elements.comment.value,
    });
    form.reset;
  }

  // I think I'm sending the tag incorrectly.
  async function pollTalks() {
    let tag = undefined;
    for (;;) {
      let response;
      try {
        response = await axios.get("/talks", {
          headers: tag && { "If-None-Match": tag, Prefer: "wait=90" },
        });
      } catch (e) {
        console.log("Request failed: " + e);
        await new Promise((resolve) => setTimeout(resolve, 500));
        continue;
      }
      if (response.status == 304) continue;
      const res = await response;
      tag = res.headers.etag;
      setTalks(res.data);
    }
  }

  useEffect(() => {
    console.log("how many times do we get in here");
    pollTalks();
  }, []);

  return (
    <>
      {talks &&
        talks.map((talk, index) => (
          <section key={index} className="talk">
            <h2>
              {talk.title}{" "}
              <button type="button" onClick={() => deleteTalk(talk.title)}>
                Delete
              </button>
            </h2>
            <div>
              by <strong>{talk.presenter}</strong>
            </div>
            <p>{talk.summary}</p>
            {talk.comments.map((comment, commentIndex) => (
              <p key={commentIndex + 10000} className="comment">
                <strong>{comment.author}</strong>: {comment.message}
              </p>
            ))}
            <form onSubmit={submitComment}>
              <input type="text" name="comment" />{" "}
              <button type="submit">Add comment</button>
            </form>
          </section>
        ))}
    </>
  );
}
