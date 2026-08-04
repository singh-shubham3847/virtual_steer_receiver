using System;
using System.Collections.Concurrent;
using System.IO;

namespace VirtualSteerReceiver.Utils
{
    public enum LogLevel
    {
        Info,
        Warning,
        Error
    }

    public sealed record LogEntry(DateTime Timestamp, LogLevel Level, string Message)
    {
        public override string ToString() => $"[{Timestamp:HH:mm:ss.fff}] [{Level}] {Message}";
    }

    public sealed class Logger
    {
        private static readonly Lazy<Logger> _instance = new(() => new Logger());
        public static Logger Instance => _instance.Value;

        public event Action<LogEntry>? LogEntryAdded;

        private readonly ConcurrentQueue<LogEntry> _logs = new();
        public ConcurrentQueue<LogEntry> Logs => _logs;

        public void Log(LogLevel level, string message)
        {
            var entry = new LogEntry(DateTime.UtcNow, level, message);
            _logs.Enqueue(entry);

            // Limit buffer size to 500 entries
            while (_logs.Count > 500)
            {
                _logs.TryDequeue(out _);
            }

            LogEntryAdded?.Invoke(entry);
        }

        public void Info(string message) => Log(LogLevel.Info, message);
        public void Warn(string message) => Log(LogLevel.Warning, message);
        public void Error(string message) => Log(LogLevel.Error, message);
    }
}
